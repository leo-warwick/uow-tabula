create table recordeddecision (
    id varchar not null,
    hib_version numeric default 0,
    spr_code varchar not null,
    sequence varchar not null,
    academic_year smallint not null,
    decision varchar not null,
    resit_period boolean not null,
    notes varchar,
    updated_by varchar not null,
    updated_date timestamp(6) not null,
    needs_writing_to_sits_since timestamp(6),
    last_written_to_sits timestamp(6),
    constraint pk_recordeddecision primary key (id)
);

-- can only have at most 2 decisions recorded per year - the original and the resit decision
create unique index ck_recordeddecision on recordeddecision (spr_code, academic_year, resit_period);


alter table progressiondecision add column status varchar not null default 'Complete';
alter table progressiondecision alter column outcome drop not null;
