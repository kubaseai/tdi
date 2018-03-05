create table EVENTS_T(id numeric primary key, version numeric, 
host varchar2(200), repo varchar2(200), proc varchar2(2000), job numeric,
cr numeric, fn numeric, rn numeric, fl numeric, tc numeric, st numeric,
d numeric, en numeric, wt numeric, et numeric, ec numeric, type varchar2(200), jts numeric, jexp numeric, jdc numeric, 
metrics clob, em clob,
markers_defs clob, markers clob, markers_msgs clob,
mk1 varchar2(4000), mk2 varchar2(4000), mk3 varchar2(4000),
mk4 varchar2(4000), mk5 varchar2(4000), mk6 varchar2(4000));;

create table EVENTS_DATA(id numeric primary key, host varchar2(200), repo varchar2(200), proc varchar2(2000),
job numeric, version numeric, 
kind varchar2(4000), data varchar2(4000), input clob, output clob, v1 numeric, v2 numeric, v3 numeric, 
v4 numeric, v5 numeric, v6 numeric);;

CREATE SEQUENCE EV_SEQ INCREMENT BY 1 START WITH 1;;
CREATE SEQUENCE EVD_SEQ INCREMENT BY 1 START WITH 1;;

begin
 execute immediate 'create or replace trigger EVD_ID_TRIGGER  '||
	'   before insert on "EVENTS_DATA" '||
	'   for each row '||
	'begin  '||
	'   if inserting then '||
	'      if :NEW."ID" is null then '||
	'         select EVD_SEQ.nextval into :NEW."ID" from dual; '||
	'      end if; '||
	'   end if; '||
	'end;';
end;;

begin
 execute immediate 'create or replace trigger EV_ID_TRIGGER  '||
	'   before insert on "EVENTS_T" '||
	'   for each row '||
	'begin  '||
	'   if inserting then '||
	'      if :NEW."ID" is null then '||
	'         select EV_SEQ.nextval into :NEW."ID" from dual; '||
	'      end if; '||
	'   end if; '||
	'end;';
end;;

create index IDX_ET_HOST on EVENTS_T(host);;
create index IDX_ET_HOST_REPO on EVENTS_T(host,repo);;
create index IDX_ET_REPO_PROC on EVENTS_T(repo,proc);;
create index IDX_ET_TYPE on EVENTS_T(type);;
create index IDX_ET_JOB on EVENTS_T(job);;
create index IDX_ED_HOST_REPO on EVENTS_DATA(host,repo);;
create index IDX_ED_REPO_PROC on EVENTS_DATA(repo,proc);;
create index IDX_ED_JOB on EVENTS_DATA(JOB);;

create index IDX_MK1 on EVENTS_T(mk1);;
create index IDX_MK2 on EVENTS_T(mk2);;
create index IDX_MK3 on EVENTS_T(mk3);;
create index IDX_MK4 on EVENTS_T(mk4);;
create index IDX_MK5 on EVENTS_T(mk5);;
create index IDX_MK6 on EVENTS_T(mk6);;

create table USAGE_T(id numeric primary key, version numeric, 
host varchar2(200), repo varchar2(200), cpu real, mem real, gc real,
st numeric);;

CREATE SEQUENCE UG_SEQ INCREMENT BY 1 START WITH 1;;

begin
 execute immediate 'create or replace trigger UG_ID_TRIGGER  '||
	'   before insert on "USAGE_T" '||
	'   for each row '||
	'begin  '||
	'   if inserting then '||
	'      if :NEW."ID" is null then '||
	'         select UG_SEQ.nextval into :NEW."ID" from dual; '||
	'      end if; '||
	'   end if; '||
	'end;';
end;;

create table PARAMS_T(id numeric primary key, version numeric, host varchar2(200),
repo varchar2(200), param varchar2(200), value varchar2(4000));;

create index IDX_PT_HOST_REPO on PARAMS_T(host,repo);;

CREATE SEQUENCE PT_SEQ INCREMENT BY 1 START WITH 1;;

begin
 execute immediate 'create or replace trigger PT_ID_TRIGGER  '||
	'   before insert on "PARAMS_T" '||
	'   for each row '||
	'begin  '||
	'   if inserting then '||
	'      if :NEW."ID" is null then '||
	'         select PT_SEQ.nextval into :NEW."ID" from dual; '||
	'      end if; '||
	'   end if; '||
	'end;';
end;;
		
create table POP_T(id numeric primary key, version numeric, host varchar2(200),
repo varchar2(200), type varchar2(200), key varchar2(4000), value clob);;

create index IDX_POP_HOST_REPO on POP_T(host,repo);;

CREATE SEQUENCE POP_SEQ INCREMENT BY 1 START WITH 1;;

begin
 execute immediate 'create or replace trigger POP_ID_TRIGGER  '||
	'   before insert on "POP_T" '||
	'   for each row '||
	'begin  '||
	'   if inserting then '||
	'      if :NEW."ID" is null then '||
	'         select POP_SEQ.nextval into :NEW."ID" from dual; '||
	'      end if; '||
	'   end if; '||
	'end;';
end;;
		