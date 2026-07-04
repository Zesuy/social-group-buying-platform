-- P2: 订单上下文聊天会话与消息

CREATE TABLE chat_conversations (
    id                    BIGINT      NOT NULL PRIMARY KEY,
    buyer_user_id         BIGINT      NOT NULL,
    leader_user_id        BIGINT      NOT NULL,
    store_id              BIGINT      NOT NULL,
    last_message_id       BIGINT      NULL,
    last_message_at       DATETIME    NULL,
    buyer_unread_count    INT         NOT NULL DEFAULT 0,
    leader_unread_count   INT         NOT NULL DEFAULT 0,
    buyer_last_read_at    DATETIME    NULL,
    leader_last_read_at   DATETIME    NULL,
    created_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_conversations_buyer_store UNIQUE (buyer_user_id, store_id)
) COMMENT='买家与店铺团长聊天会话';

CREATE INDEX idx_chat_conversations_buyer_updated ON chat_conversations (buyer_user_id, updated_at);
CREATE INDEX idx_chat_conversations_leader_updated ON chat_conversations (leader_user_id, updated_at);
CREATE INDEX idx_chat_conversations_store ON chat_conversations (store_id);

CREATE TABLE chat_messages (
    id                  BIGINT       NOT NULL PRIMARY KEY,
    conversation_id     BIGINT       NOT NULL,
    sender_user_id      BIGINT       NULL,
    sender_role         VARCHAR(16)  NOT NULL COMMENT 'buyer / leader / system',
    message_type        VARCHAR(16)  NOT NULL COMMENT 'text / image / card',
    content             VARCHAR(1000) NULL,
    image_asset_id      BIGINT       NULL,
    image_url           VARCHAR(512) NULL,
    card_type           VARCHAR(32)  NULL,
    card_payload        TEXT         NULL,
    related_order_id    BIGINT       NULL,
    client_message_id   VARCHAR(128) NULL,
    created_at          DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_chat_messages_client UNIQUE (conversation_id, sender_user_id, client_message_id)
) COMMENT='聊天消息表';

CREATE INDEX idx_chat_messages_conversation_created ON chat_messages (conversation_id, created_at);
CREATE INDEX idx_chat_messages_conversation_id ON chat_messages (conversation_id, id);
CREATE INDEX idx_chat_messages_related_order ON chat_messages (related_order_id);
