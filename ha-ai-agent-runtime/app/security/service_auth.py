"""创建仅信任批准 CA 且携带 Runtime 工作负载证书的 TLS 上下文。"""

import ssl

from app.config import RuntimeSettings


def create_laya_ssl_context(settings: RuntimeSettings) -> ssl.SSLContext:
    """按受控部署配置加载 mTLS 证书；缺失或无效时由启动/构造过程失败关闭。"""
    ca_cert, client_cert, client_key = settings.require_laya_mtls()
    context = ssl.create_default_context(ssl.Purpose.SERVER_AUTH, cafile=ca_cert)
    context.minimum_version = ssl.TLSVersion.TLSv1_2
    context.load_cert_chain(certfile=client_cert, keyfile=client_key)
    return context
