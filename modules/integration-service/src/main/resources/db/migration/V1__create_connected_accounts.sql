CREATE TABLE connected_accounts (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL,
    provider VARCHAR(255) NOT NULL,
    provider_account_id VARCHAR(255),
    email VARCHAR(255),
    scopes VARCHAR(1000),
    encrypted_refresh_token VARCHAR(4000) NOT NULL,
    connected_at TIMESTAMP WITH TIME ZONE,
    updated_at TIMESTAMP WITH TIME ZONE,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT uk_connected_accounts_user_provider UNIQUE (user_id, provider)
);

CREATE INDEX idx_connected_accounts_user_id ON connected_accounts(user_id);

CREATE TABLE oauth_states (
    id UUID PRIMARY KEY,
    state VARCHAR(255) NOT NULL UNIQUE,
    user_id UUID NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE
);
