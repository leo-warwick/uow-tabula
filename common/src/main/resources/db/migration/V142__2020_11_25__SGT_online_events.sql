alter table SmallGroupEvent
    add column deliveryMethod varchar,
    add column onlineDeliveryUrl varchar,
    add column onlinePlatform varchar;

update SmallGroupEvent set deliveryMethod = 'Hybrid' where deliveryMethod is null;

alter table SmallGroupEvent alter column deliveryMethod set not null;

alter table SmallGroupSet
    add column defaultDeliveryMethod varchar,
    add column defaultOnlineDeliveryUrl varchar,
    add column defaultOnlinePlatform varchar;
