"""Laya SystemOne API 的严格请求与响应模型。"""

from __future__ import annotations

import re
from typing import Annotated, Literal

from pydantic import BaseModel, ConfigDict, Field, field_validator


class StrictModel(BaseModel):
    """拒绝未批准字段并保持 JSON 类型严格。"""

    model_config = ConfigDict(extra="forbid", strict=True)


class ChoiceQuestion(StrictModel):
    """选择题问题模板。"""

    type: Literal["choice"]
    instructions: str = Field(min_length=1, max_length=1000)
    criteria: dict[str, str] = Field(min_length=2, max_length=32)

    @field_validator("criteria")
    @classmethod
    def validate_choice_criteria(cls, value: dict[str, str]) -> dict[str, str]:
        """校验标签标识符合契约规定。"""
        if any(re.fullmatch(r"[a-z][a-z0-9_-]{0,63}", label) is None for label in value):
            raise ValueError("选择标签标识格式无效")
        if any(len(text) > 256 for text in value.values()):
            raise ValueError("选择标签说明超过 256 字符")
        return value


class ScoreQuestion(StrictModel):
    """按顺序定义评分等级的问题模板。"""

    type: Literal["score"]
    instructions: str = Field(min_length=1, max_length=1000)
    criteria: list[str] = Field(min_length=2, max_length=10)

    @field_validator("criteria")
    @classmethod
    def validate_score_criteria(cls, value: list[str]) -> list[str]:
        """校验每个有序评分等级的文本长度。"""
        if any(not item or len(item) > 128 for item in value):
            raise ValueError("评分等级必须为 1 至 128 字符")
        return value


class NoulQuestion(StrictModel):
    """二元判断问题模板。"""

    type: Literal["noul"]
    instructions: str = Field(min_length=1, max_length=1000)


Question = Annotated[
    ChoiceQuestion | ScoreQuestion | NoulQuestion,
    Field(discriminator="type"),
]


class DecisionRequest(StrictModel):
    """Runtime 到 Laya 的受限输入；模型选择不属于请求字段。"""

    state: str = Field(max_length=4000)
    questions: dict[str, Question] = Field(min_length=1, max_length=8)

    @field_validator("questions")
    @classmethod
    def validate_question_ids(cls, value: dict[str, Question]) -> dict[str, Question]:
        """确保问题 ID 符合稳定的小写标识格式。"""
        if any(re.fullmatch(r"[a-z][a-z0-9_]{0,63}", question_id) is None for question_id in value):
            raise ValueError("问题 ID 格式无效")
        return value


class ActionMetadata(StrictModel):
    """保留上游动作置信元数据，不赋予其执行授权。"""

    act_probability: float = Field(ge=0, le=1)


class ChoiceAnswer(StrictModel):
    """上游 choice 类型推理结果。"""

    type: Literal["choice"]
    choice: str
    confidence: float = Field(ge=0, le=1)
    probabilities: dict[str, float]
    action: ActionMetadata


class ScoreAnswer(StrictModel):
    """上游 score 类型零起始 ordinal 期望结果。"""

    type: Literal["score"]
    score: float = Field(ge=0, le=9)
    legend: dict[str, str]
    probabilities: dict[str, float]
    confidence: float = Field(ge=0, le=1)
    action: ActionMetadata


class NoulAnswer(StrictModel):
    """上游 noul 类型推理结果。"""

    type: Literal["noul"]
    noul: float = Field(ge=0, le=1)
    confidence: float = Field(ge=0, le=1)
    action: ActionMetadata


Answer = Annotated[
    ChoiceAnswer | ScoreAnswer | NoulAnswer,
    Field(discriminator="type"),
]


class Usage(StrictModel):
    """上游推理用量统计。"""

    input_tokens: int = Field(ge=0)
    output_tokens: int = Field(ge=0)


class RoutingMetadata(BaseModel):
    """上游路由元数据；保留上游新增字段但固定校验 checkpoint。"""

    model_config = ConfigDict(extra="allow", strict=True)

    model: Literal["multilingual"]
    repo: str | None = None
    reason: str | None = Field(default=None, max_length=512)


class DecisionResponse(StrictModel):
    """Laya 推理结果；调用方还需按原始请求验证 ID 和允许值。"""

    model: str = Field(min_length=1)
    answers: dict[str, Answer] = Field(min_length=1)
    usage: Usage
    routing: RoutingMetadata
    latency_ms: float | None = Field(default=None, ge=0)

    def validate_against(self, request: DecisionRequest) -> None:
        """将响应问题、类型和标签映射与本次批准请求逐项绑定。"""
        if set(self.answers) != set(request.questions):
            raise ValueError("推理结果问题 ID 与请求不一致")

        for question_id, question in request.questions.items():
            answer = self.answers[question_id]
            if answer.type != question.type:
                raise ValueError("推理结果类型与请求问题不一致")

            if isinstance(question, ChoiceQuestion) and isinstance(answer, ChoiceAnswer):
                allowed_labels = set(question.criteria)
                if answer.choice not in allowed_labels or set(answer.probabilities) != allowed_labels:
                    raise ValueError("选择结果或概率标签不在请求允许范围内")

            if isinstance(question, ScoreQuestion) and isinstance(answer, ScoreAnswer):
                expected_indices = {str(index) for index in range(len(question.criteria))}
                if not 0 <= answer.score <= len(question.criteria) - 1:
                    raise ValueError("评分结果超出请求等级范围")
                if set(answer.legend) != expected_indices or set(answer.probabilities) != expected_indices:
                    raise ValueError("评分结果索引与请求等级数量不一致")
                if any(answer.legend[str(index)] != text for index, text in enumerate(question.criteria)):
                    raise ValueError("评分结果等级映射与请求模板不一致")

