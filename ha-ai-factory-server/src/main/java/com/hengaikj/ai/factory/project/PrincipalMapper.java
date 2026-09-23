package com.hengaikj.ai.factory.project;

import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/** 通过OIDC issuer+subject维护稳定的内部主体引用。 */
@Mapper
public interface PrincipalMapper {
    /** 按OIDC身份唯一键更新展示信息，但保留既有停用状态。 */
    @Insert("INSERT INTO principals(principal_ref,principal_type,oidc_issuer,oidc_subject,display_name,is_active,last_authenticated_at) " +
            "VALUES(#{principalRef},'HUMAN',#{issuer},#{subject},#{displayName},TRUE,NOW(3)) " +
            "ON DUPLICATE KEY UPDATE display_name=VALUES(display_name),last_authenticated_at=NOW(3)")
    int upsertOidcHuman(@Param("principalRef") String principalRef, @Param("issuer") String issuer,
                        @Param("subject") String subject, @Param("displayName") String displayName);

    /** 只解析当前仍启用的已登记OIDC主体。 */
    @Select("SELECT principal_ref FROM principals WHERE oidc_issuer=#{issuer} AND oidc_subject=#{subject} AND is_active=TRUE")
    String findActiveRef(@Param("issuer") String issuer, @Param("subject") String subject);
}
