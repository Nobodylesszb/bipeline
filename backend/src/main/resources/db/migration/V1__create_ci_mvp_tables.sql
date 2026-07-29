create table code_sources (
    id uuid primary key,
    name varchar(120) not null,
    provider varchar(32) not null,
    base_url varchar(500) not null,
    auth_type varchar(32) not null,
    username varchar(200),
    secret_plain text,
    verification_status varchar(32) not null,
    last_verified_at timestamptz,
    last_verification_message varchar(1000),
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_code_sources_name unique (name),
    constraint ck_code_sources_provider check (
        provider in ('GITLAB', 'GITHUB', 'GITEA', 'GENERIC_GIT')
    ),
    constraint ck_code_sources_auth_type check (
        auth_type in ('NONE', 'USERNAME_PASSWORD', 'ACCESS_TOKEN', 'DEPLOY_TOKEN', 'SSH_KEY')
    ),
    constraint ck_code_sources_verification_status check (
        verification_status in ('UNVERIFIED', 'VERIFIED', 'FAILED')
    )
);

create table projects (
    id uuid primary key,
    name varchar(120) not null,
    description varchar(1000),
    code_source_id uuid not null references code_sources (id),
    status varchar(32) not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_projects_name unique (name),
    constraint ck_projects_status check (status in ('ACTIVE', 'ARCHIVED'))
);

create table repositories (
    id uuid primary key,
    project_id uuid not null references projects (id) on delete cascade,
    remote_path varchar(500) not null,
    remote_url varchar(1000) not null,
    default_branch varchar(200) not null,
    context_directory varchar(500) not null,
    last_resolved_revision varchar(200),
    last_fetched_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_repositories_project unique (project_id)
);

create table build_profiles (
    id uuid primary key,
    name varchar(120) not null,
    language varchar(40) not null,
    template_key varchar(120) not null,
    template_version varchar(40) not null,
    description varchar(1000),
    schema_json jsonb not null,
    default_config_json jsonb not null,
    enabled boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_build_profiles_template unique (template_key, template_version)
);

create table plugin_contracts (
    id uuid primary key,
    step_type varchar(80) not null,
    plugin_version varchar(40) not null,
    display_name varchar(120) not null,
    description varchar(1000),
    input_schema_json jsonb not null,
    output_schema_json jsonb not null,
    enabled boolean not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_plugin_contracts_type_version unique (step_type, plugin_version)
);

create table pipeline_configurations (
    id uuid primary key,
    project_id uuid not null references projects (id) on delete cascade,
    name varchar(120) not null,
    description varchar(1000),
    default_branch varchar(200) not null,
    context_directory varchar(500) not null,
    build_profile_id uuid not null references build_profiles (id),
    build_profile_version varchar(40) not null,
    config_json jsonb not null,
    trigger_mode varchar(32) not null,
    status varchar(32) not null,
    version integer not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_pipeline_configurations_project_name unique (project_id, name),
    constraint ck_pipeline_configurations_trigger_mode check (
        trigger_mode in ('MANUAL', 'WEBHOOK', 'SCHEDULE')
    ),
    constraint ck_pipeline_configurations_status check (
        status in ('DRAFT', 'ACTIVE', 'DISABLED', 'ARCHIVED')
    )
);

create table pipeline_steps (
    id uuid primary key,
    pipeline_id uuid not null references pipeline_configurations (id) on delete cascade,
    step_key varchar(120) not null,
    name varchar(120) not null,
    stage_name varchar(120) not null,
    step_type varchar(80) not null,
    plugin_version varchar(40) not null,
    step_order integer not null,
    config_json jsonb not null,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_pipeline_steps_key unique (pipeline_id, step_key),
    constraint uk_pipeline_steps_order unique (pipeline_id, step_order)
);

create table pipeline_runs (
    id uuid primary key,
    pipeline_id uuid not null references pipeline_configurations (id),
    project_id uuid not null references projects (id),
    run_number integer not null,
    status varchar(32) not null,
    trigger_type varchar(32) not null,
    branch varchar(200) not null,
    revision varchar(200),
    configuration_snapshot_json jsonb not null,
    execution_engine varchar(40) not null,
    external_run_ref varchar(500),
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_pipeline_runs_number unique (pipeline_id, run_number),
    constraint ck_pipeline_runs_status check (
        status in ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED')
    ),
    constraint ck_pipeline_runs_trigger_type check (
        trigger_type in ('MANUAL', 'WEBHOOK', 'SCHEDULE')
    ),
    constraint ck_pipeline_runs_execution_engine check (
        execution_engine in ('TEKTON')
    )
);

create table step_runs (
    id uuid primary key,
    pipeline_run_id uuid not null references pipeline_runs (id) on delete cascade,
    step_key varchar(120) not null,
    name varchar(120) not null,
    status varchar(32) not null,
    external_step_ref varchar(500),
    log_ref varchar(1000),
    started_at timestamptz,
    finished_at timestamptz,
    created_at timestamptz not null,
    updated_at timestamptz not null,
    constraint uk_step_runs_key unique (pipeline_run_id, step_key),
    constraint ck_step_runs_status check (
        status in ('PENDING', 'RUNNING', 'SUCCEEDED', 'FAILED', 'CANCELED', 'SKIPPED')
    )
);

create table build_results (
    id uuid primary key,
    pipeline_run_id uuid not null references pipeline_runs (id) on delete cascade,
    project_id uuid not null references projects (id),
    image_name varchar(500),
    image_tag varchar(200),
    image_digest varchar(200),
    artifact_path varchar(1000),
    test_summary_json jsonb not null,
    quality_summary_json jsonb not null,
    created_at timestamptz not null,
    constraint uk_build_results_run unique (pipeline_run_id)
);

create index idx_projects_code_source_id on projects (code_source_id);
create index idx_pipeline_configurations_project_id on pipeline_configurations (project_id);
create index idx_pipeline_runs_project_id on pipeline_runs (project_id);
create index idx_pipeline_runs_status on pipeline_runs (status);
create index idx_step_runs_pipeline_run_id on step_runs (pipeline_run_id);
create index idx_build_results_project_id on build_results (project_id);
