alter table pipeline_runs
    drop constraint if exists ck_pipeline_runs_execution_engine;

alter table pipeline_runs
    add constraint ck_pipeline_runs_execution_engine check (
        execution_engine in ('JENKINS')
    );
