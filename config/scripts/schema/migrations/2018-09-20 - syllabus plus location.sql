CREATE TABLE SYLLABUSPLUSLOCATION (
  ID              nvarchar2(255) not null,
  UPSTREAM_NAME   nvarchar2(255) not null,
  NAME            nvarchar2(255) not null,
  MAP_LOCATION_ID nvarchar2(255) not null,

  constraint "SYLLABUSPLUSLOCATION_PK" primary key (ID)
);

create index idx_syllabuspluslocation
  on SYLLABUSPLUSLOCATION (UPSTREAM_NAME);

