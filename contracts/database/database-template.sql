-- 所有表和字段必须增加中文COMMENT

CREATE TABLE example_table (
    id BIGINT PRIMARY KEY COMMENT '主键ID',
    name VARCHAR(128) COMMENT '名称',
    created_at DATETIME COMMENT '创建时间'
) COMMENT='示例业务表';
