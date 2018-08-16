
##drop database if exists policy_db_test;
##create database policy_db_test;
use policy_db_test;

## Policy table defines:
## the policyid, which is auto-increment and not necessarily specified, 
## the author defined through TIPPERS,
## the start time stamp for when the policy will start being active,
## the end time stamp for when the policy will stop being active.
create table policy (
	policyid INT primary key not null auto_increment,
    author varchar(30) not null,
    startdatetime varchar(20) not null,
    enddatetime varchar(20) not null
);

## Devices table specifies the devices specified in policies.
create table devices (
    deviceid varchar(15) primary key not null
);

## Devices_policy table associates devices with policies to give info
## for which devices are associated to each policy.
create table devices_policy (
	id int primary key auto_increment not null,
	policy_id int not null, 
	device_id varchar(15) not null, 
	foreign key (device_id) references devices(deviceid),
	foreign key (policy_id) references policy(policyid)
);

## Conditionpol table specifies condition for a particular policy.
## This defines the operator, the threshold value, and the policy id.
## Condition for data capture policy checks if particular devices' numerical data
## can be compared to the threshold value by the operator. 
## Device data operator threshold value is the structure for the condition.
create table conditionpol (
	id int primary key auto_increment not null,
    policyid int not null,
    operator varchar(2) not null,
    threshhold int not null,
    foreign key (policyid) references policy(policyid)
);

## Userpolicy table links users with the policy that they are providing
## consent to and the consent that they are providing.
## When the user consents to a policy through the IoTA application,
## this table is updated with the email id of the user, the policy, and
## the consent for that particular policy.
create table userpolicy (
	id int primary key not null auto_increment,
	policyid int not null,
    user varchar(50) not null,
    consent varchar(10) 
);

## Logging table saves metadata for logs including a logid,
## the type of the log, state or payload, the state of the log,
## for state logs only, the deviceid for the device the log comes from,
## start timestamp for when the state or payload began,
## the end timestamp for when the state or payload completes,
## the contextid for a context only for state logs,
## the payload which specifies the payload of the device's data,
## only for payload logs.
create table logging (
	logid int primary key not null auto_increment,
    log_type varchar(10) not null,
    state varchar(5),
    deviceid varchar(15) not null,
    startstamp varchar(20) not null,
    endstamp varchar(20) not null,
    contextid int,
    payload int
);

## ContractHend specifies the contractid/policyid and the Hend that is 
## associated with that contract. The Hend is simply the hash value for 
## the concatenation for a particular log entry and the hash of all the 
## previous logs.
create table contractHend (
	contractid int primary key not null,
    Hend int not null
);

## Contractlog links the contractid/policyid and the Hend values to 
## particular logs that have those values. This allows for contractid
## and Hend to be an extra metadata value for each log.
create table contractlog (
	id int primary key not null auto_increment,
    logid int not null,
    contractid int not null,
    Hend int not null,
    foreign key (logid) references logging(logid),
    foreign key (contractid) references contractHend(contractid),
    foreign key (Hend) references contractHend(Hend)
);



## what to do if policy is created and is to be effective only in future?

