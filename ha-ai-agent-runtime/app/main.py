"""创建不暴露未获批业务路由的 FastAPI 运行容器。"""

from fastapi import FastAPI


def create_app() -> FastAPI:
    """创建关闭自动文档路由的内部应用，业务路由待对应 API Contract 批准。"""
    return FastAPI(
        title="HA AI Agent Runtime",
        docs_url=None,
        redoc_url=None,
        openapi_url=None,
    )


app = create_app()
