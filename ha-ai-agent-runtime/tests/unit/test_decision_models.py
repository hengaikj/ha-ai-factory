import pytest
from pydantic import ValidationError

from app.decision.models import DecisionRequest, DecisionResponse


def valid_request() -> dict:
    return {
        "state": "脱敏后的任务摘要",
        "questions": {
            "scope_fit": {
                "type": "choice",
                "instructions": "选择最符合的范围状态",
                "criteria": {"in_scope": "范围内", "out_scope": "范围外"},
            },
            "quality_score": {
                "type": "score",
                "instructions": "评估交付质量",
                "criteria": ["不符合", "部分符合", "符合"],
            },
            "needs_followup": {
                "type": "noul",
                "instructions": "判断是否需要人工跟进",
            },
        },
    }


def valid_response() -> dict:
    return {
        "model": "laya-rl-agent",
        "answers": {
            "scope_fit": {
                "type": "choice",
                "choice": "in_scope",
                "confidence": 0.9,
                "probabilities": {"in_scope": 0.9, "out_scope": 0.1},
                "action": {"act_probability": 0.01},
            },
            "quality_score": {
                "type": "score",
                "score": 1.7,
                "legend": {"0": "不符合", "1": "部分符合", "2": "符合"},
                "probabilities": {"0": 0.1, "1": 0.1, "2": 0.8},
                "confidence": 0.7,
                "action": {"act_probability": 0.01},
            },
            "needs_followup": {
                "type": "noul",
                "noul": 0.2,
                "confidence": 0.8,
                "action": {"act_probability": 0.01},
            },
        },
        "usage": {"input_tokens": 42, "output_tokens": 0},
        "routing": {"model": "multilingual", "repo": "approved-checkpoint"},
    }


def test_request_accepts_contract_fields_without_caller_model_selection() -> None:
    request = DecisionRequest.model_validate(valid_request())

    assert request.state == "脱敏后的任务摘要"
    assert request.questions["quality_score"].type == "score"


def test_request_rejects_unapproved_fields_and_invalid_question_limits() -> None:
    payload = valid_request()
    payload["model"] = "english"
    with pytest.raises(ValidationError):
        DecisionRequest.model_validate(payload)

    payload = valid_request()
    payload["questions"] = {}
    with pytest.raises(ValidationError):
        DecisionRequest.model_validate(payload)


def test_response_accepts_upstream_answer_shapes_and_validates_against_request() -> None:
    request = DecisionRequest.model_validate(valid_request())
    response = DecisionResponse.model_validate(valid_response())

    response.validate_against(request)
    assert response.routing.model == "multilingual"
    assert response.answers["quality_score"].score == 1.7


@pytest.mark.parametrize(
    "mutate",
    [
        lambda payload: payload["answers"]["scope_fit"].update(choice="unknown"),
        lambda payload: payload["answers"]["quality_score"].update(score=3.2),
        lambda payload: payload["answers"]["quality_score"]["legend"].update({"1": "错误等级"}),
        lambda payload: payload["answers"]["quality_score"]["probabilities"].update({"3": 0.1}),
    ],
)
def test_response_rejects_unapproved_or_mismatched_answer_values(mutate) -> None:
    request = DecisionRequest.model_validate(valid_request())
    payload = valid_response()
    mutate(payload)
    response = DecisionResponse.model_validate(payload)

    with pytest.raises(ValueError):
        response.validate_against(request)


def test_response_rejects_unknown_answer_fields() -> None:
    payload = valid_response()
    payload["answers"]["needs_followup"]["extra"] = "not allowed"

    with pytest.raises(ValidationError):
        DecisionResponse.model_validate(payload)


def test_response_rejects_unknown_model_checkpoint() -> None:
    payload = valid_response()
    payload["routing"]["model"] = "english"

    with pytest.raises(ValidationError):
        DecisionResponse.model_validate(payload)
