alter table pipeline_runs drop constraint ck_pipeline_runs_v6_status;

alter table pipeline_runs add constraint ck_pipeline_runs_v8_status check (
    status in ('PENDING', 'QUEUED', 'RUNNING', 'SUCCESS', 'FAILED', 'CANCELED')
);
