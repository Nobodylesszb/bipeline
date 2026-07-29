alter table code_sources add column id_v2 bigint;
create sequence code_sources_id_seq;
update code_sources set id_v2 = nextval('code_sources_id_seq');
alter table code_sources alter column id_v2 set not null;
alter table code_sources alter column id_v2 set default nextval('code_sources_id_seq');
alter sequence code_sources_id_seq owned by code_sources.id_v2;

alter table projects add column id_v2 bigint;
alter table projects add column code_source_id_v2 bigint;
create sequence projects_id_seq;
update projects set id_v2 = nextval('projects_id_seq');
update projects
set code_source_id_v2 = code_sources.id_v2
from code_sources
where projects.code_source_id = code_sources.id;
alter table projects alter column id_v2 set not null;
alter table projects alter column code_source_id_v2 set not null;
alter table projects alter column id_v2 set default nextval('projects_id_seq');
alter sequence projects_id_seq owned by projects.id_v2;

alter table repositories add column id_v2 bigint;
alter table repositories add column project_id_v2 bigint;
create sequence repositories_id_seq;
update repositories set id_v2 = nextval('repositories_id_seq');
update repositories
set project_id_v2 = projects.id_v2
from projects
where repositories.project_id = projects.id;
alter table repositories alter column id_v2 set not null;
alter table repositories alter column project_id_v2 set not null;
alter table repositories alter column id_v2 set default nextval('repositories_id_seq');
alter sequence repositories_id_seq owned by repositories.id_v2;

alter table build_profiles add column id_v2 bigint;
create sequence build_profiles_id_seq;
update build_profiles set id_v2 = nextval('build_profiles_id_seq');
alter table build_profiles alter column id_v2 set not null;
alter table build_profiles alter column id_v2 set default nextval('build_profiles_id_seq');
alter sequence build_profiles_id_seq owned by build_profiles.id_v2;

alter table plugin_contracts add column id_v2 bigint;
create sequence plugin_contracts_id_seq;
update plugin_contracts set id_v2 = nextval('plugin_contracts_id_seq');
alter table plugin_contracts alter column id_v2 set not null;
alter table plugin_contracts alter column id_v2 set default nextval('plugin_contracts_id_seq');
alter sequence plugin_contracts_id_seq owned by plugin_contracts.id_v2;

alter table pipeline_configurations add column id_v2 bigint;
alter table pipeline_configurations add column project_id_v2 bigint;
alter table pipeline_configurations add column build_profile_id_v2 bigint;
create sequence pipeline_configurations_id_seq;
update pipeline_configurations set id_v2 = nextval('pipeline_configurations_id_seq');
update pipeline_configurations
set project_id_v2 = projects.id_v2
from projects
where pipeline_configurations.project_id = projects.id;
update pipeline_configurations
set build_profile_id_v2 = build_profiles.id_v2
from build_profiles
where pipeline_configurations.build_profile_id = build_profiles.id;
alter table pipeline_configurations alter column id_v2 set not null;
alter table pipeline_configurations alter column project_id_v2 set not null;
alter table pipeline_configurations alter column build_profile_id_v2 set not null;
alter table pipeline_configurations alter column id_v2 set default nextval('pipeline_configurations_id_seq');
alter sequence pipeline_configurations_id_seq owned by pipeline_configurations.id_v2;

alter table pipeline_steps add column id_v2 bigint;
alter table pipeline_steps add column pipeline_id_v2 bigint;
create sequence pipeline_steps_id_seq;
update pipeline_steps set id_v2 = nextval('pipeline_steps_id_seq');
update pipeline_steps
set pipeline_id_v2 = pipeline_configurations.id_v2
from pipeline_configurations
where pipeline_steps.pipeline_id = pipeline_configurations.id;
alter table pipeline_steps alter column id_v2 set not null;
alter table pipeline_steps alter column pipeline_id_v2 set not null;
alter table pipeline_steps alter column id_v2 set default nextval('pipeline_steps_id_seq');
alter sequence pipeline_steps_id_seq owned by pipeline_steps.id_v2;

alter table pipeline_runs add column id_v2 bigint;
alter table pipeline_runs add column pipeline_id_v2 bigint;
alter table pipeline_runs add column project_id_v2 bigint;
create sequence pipeline_runs_id_seq;
update pipeline_runs set id_v2 = nextval('pipeline_runs_id_seq');
update pipeline_runs
set pipeline_id_v2 = pipeline_configurations.id_v2
from pipeline_configurations
where pipeline_runs.pipeline_id = pipeline_configurations.id;
update pipeline_runs
set project_id_v2 = projects.id_v2
from projects
where pipeline_runs.project_id = projects.id;
alter table pipeline_runs alter column id_v2 set not null;
alter table pipeline_runs alter column pipeline_id_v2 set not null;
alter table pipeline_runs alter column project_id_v2 set not null;
alter table pipeline_runs alter column id_v2 set default nextval('pipeline_runs_id_seq');
alter sequence pipeline_runs_id_seq owned by pipeline_runs.id_v2;

alter table step_runs add column id_v2 bigint;
alter table step_runs add column pipeline_run_id_v2 bigint;
create sequence step_runs_id_seq;
update step_runs set id_v2 = nextval('step_runs_id_seq');
update step_runs
set pipeline_run_id_v2 = pipeline_runs.id_v2
from pipeline_runs
where step_runs.pipeline_run_id = pipeline_runs.id;
alter table step_runs alter column id_v2 set not null;
alter table step_runs alter column pipeline_run_id_v2 set not null;
alter table step_runs alter column id_v2 set default nextval('step_runs_id_seq');
alter sequence step_runs_id_seq owned by step_runs.id_v2;

alter table build_results add column id_v2 bigint;
alter table build_results add column pipeline_run_id_v2 bigint;
alter table build_results add column project_id_v2 bigint;
create sequence build_results_id_seq;
update build_results set id_v2 = nextval('build_results_id_seq');
update build_results
set pipeline_run_id_v2 = pipeline_runs.id_v2
from pipeline_runs
where build_results.pipeline_run_id = pipeline_runs.id;
update build_results
set project_id_v2 = projects.id_v2
from projects
where build_results.project_id = projects.id;
alter table build_results alter column id_v2 set not null;
alter table build_results alter column pipeline_run_id_v2 set not null;
alter table build_results alter column project_id_v2 set not null;
alter table build_results alter column id_v2 set default nextval('build_results_id_seq');
alter sequence build_results_id_seq owned by build_results.id_v2;

drop index if exists idx_projects_code_source_id;
drop index if exists idx_pipeline_configurations_project_id;
drop index if exists idx_pipeline_runs_project_id;
drop index if exists idx_step_runs_pipeline_run_id;
drop index if exists idx_build_results_project_id;

alter table build_results drop constraint if exists build_results_pipeline_run_id_fkey;
alter table build_results drop constraint if exists build_results_project_id_fkey;
alter table step_runs drop constraint if exists step_runs_pipeline_run_id_fkey;
alter table pipeline_runs drop constraint if exists pipeline_runs_pipeline_id_fkey;
alter table pipeline_runs drop constraint if exists pipeline_runs_project_id_fkey;
alter table pipeline_steps drop constraint if exists pipeline_steps_pipeline_id_fkey;
alter table pipeline_configurations drop constraint if exists pipeline_configurations_project_id_fkey;
alter table pipeline_configurations drop constraint if exists pipeline_configurations_build_profile_id_fkey;
alter table repositories drop constraint if exists repositories_project_id_fkey;
alter table projects drop constraint if exists projects_code_source_id_fkey;

alter table build_results drop constraint if exists uk_build_results_run;
alter table step_runs drop constraint if exists uk_step_runs_key;
alter table pipeline_runs drop constraint if exists uk_pipeline_runs_number;
alter table pipeline_steps drop constraint if exists uk_pipeline_steps_key;
alter table pipeline_steps drop constraint if exists uk_pipeline_steps_order;
alter table pipeline_configurations drop constraint if exists uk_pipeline_configurations_project_name;
alter table repositories drop constraint if exists uk_repositories_project;

alter table build_results drop constraint if exists build_results_pkey;
alter table step_runs drop constraint if exists step_runs_pkey;
alter table pipeline_runs drop constraint if exists pipeline_runs_pkey;
alter table pipeline_steps drop constraint if exists pipeline_steps_pkey;
alter table pipeline_configurations drop constraint if exists pipeline_configurations_pkey;
alter table plugin_contracts drop constraint if exists plugin_contracts_pkey;
alter table build_profiles drop constraint if exists build_profiles_pkey;
alter table repositories drop constraint if exists repositories_pkey;
alter table projects drop constraint if exists projects_pkey;
alter table code_sources drop constraint if exists code_sources_pkey;

alter table build_results drop column pipeline_run_id;
alter table build_results drop column project_id;
alter table build_results drop column id;
alter table build_results rename column pipeline_run_id_v2 to pipeline_run_id;
alter table build_results rename column project_id_v2 to project_id;
alter table build_results rename column id_v2 to id;

alter table step_runs drop column pipeline_run_id;
alter table step_runs drop column id;
alter table step_runs rename column pipeline_run_id_v2 to pipeline_run_id;
alter table step_runs rename column id_v2 to id;

alter table pipeline_runs drop column pipeline_id;
alter table pipeline_runs drop column project_id;
alter table pipeline_runs drop column id;
alter table pipeline_runs rename column pipeline_id_v2 to pipeline_id;
alter table pipeline_runs rename column project_id_v2 to project_id;
alter table pipeline_runs rename column id_v2 to id;

alter table pipeline_steps drop column pipeline_id;
alter table pipeline_steps drop column id;
alter table pipeline_steps rename column pipeline_id_v2 to pipeline_id;
alter table pipeline_steps rename column id_v2 to id;

alter table pipeline_configurations drop column project_id;
alter table pipeline_configurations drop column build_profile_id;
alter table pipeline_configurations drop column id;
alter table pipeline_configurations rename column project_id_v2 to project_id;
alter table pipeline_configurations rename column build_profile_id_v2 to build_profile_id;
alter table pipeline_configurations rename column id_v2 to id;

alter table plugin_contracts drop column id;
alter table plugin_contracts rename column id_v2 to id;

alter table build_profiles drop column id;
alter table build_profiles rename column id_v2 to id;

alter table repositories drop column project_id;
alter table repositories drop column id;
alter table repositories rename column project_id_v2 to project_id;
alter table repositories rename column id_v2 to id;

alter table projects drop column code_source_id;
alter table projects drop column id;
alter table projects rename column code_source_id_v2 to code_source_id;
alter table projects rename column id_v2 to id;

alter table code_sources drop column id;
alter table code_sources rename column id_v2 to id;

alter table code_sources add constraint code_sources_pkey primary key (id);
alter table projects add constraint projects_pkey primary key (id);
alter table repositories add constraint repositories_pkey primary key (id);
alter table build_profiles add constraint build_profiles_pkey primary key (id);
alter table plugin_contracts add constraint plugin_contracts_pkey primary key (id);
alter table pipeline_configurations add constraint pipeline_configurations_pkey primary key (id);
alter table pipeline_steps add constraint pipeline_steps_pkey primary key (id);
alter table pipeline_runs add constraint pipeline_runs_pkey primary key (id);
alter table step_runs add constraint step_runs_pkey primary key (id);
alter table build_results add constraint build_results_pkey primary key (id);

alter table projects
    add constraint projects_code_source_id_fkey
    foreign key (code_source_id) references code_sources (id);

alter table repositories
    add constraint repositories_project_id_fkey
    foreign key (project_id) references projects (id) on delete cascade;

alter table pipeline_configurations
    add constraint pipeline_configurations_project_id_fkey
    foreign key (project_id) references projects (id) on delete cascade;

alter table pipeline_configurations
    add constraint pipeline_configurations_build_profile_id_fkey
    foreign key (build_profile_id) references build_profiles (id);

alter table pipeline_steps
    add constraint pipeline_steps_pipeline_id_fkey
    foreign key (pipeline_id) references pipeline_configurations (id) on delete cascade;

alter table pipeline_runs
    add constraint pipeline_runs_pipeline_id_fkey
    foreign key (pipeline_id) references pipeline_configurations (id);

alter table pipeline_runs
    add constraint pipeline_runs_project_id_fkey
    foreign key (project_id) references projects (id);

alter table step_runs
    add constraint step_runs_pipeline_run_id_fkey
    foreign key (pipeline_run_id) references pipeline_runs (id) on delete cascade;

alter table build_results
    add constraint build_results_pipeline_run_id_fkey
    foreign key (pipeline_run_id) references pipeline_runs (id) on delete cascade;

alter table build_results
    add constraint build_results_project_id_fkey
    foreign key (project_id) references projects (id);

alter table repositories
    add constraint uk_repositories_project unique (project_id);

alter table pipeline_configurations
    add constraint uk_pipeline_configurations_project_name unique (project_id, name);

alter table pipeline_steps
    add constraint uk_pipeline_steps_key unique (pipeline_id, step_key);

alter table pipeline_steps
    add constraint uk_pipeline_steps_order unique (pipeline_id, step_order);

alter table pipeline_runs
    add constraint uk_pipeline_runs_number unique (pipeline_id, run_number);

alter table step_runs
    add constraint uk_step_runs_key unique (pipeline_run_id, step_key);

alter table build_results
    add constraint uk_build_results_run unique (pipeline_run_id);

create index if not exists idx_projects_code_source_id on projects (code_source_id);
create index if not exists idx_pipeline_configurations_project_id on pipeline_configurations (project_id);
create index if not exists idx_pipeline_runs_project_id on pipeline_runs (project_id);
create index if not exists idx_pipeline_runs_status on pipeline_runs (status);
create index if not exists idx_step_runs_pipeline_run_id on step_runs (pipeline_run_id);
create index if not exists idx_build_results_project_id on build_results (project_id);
