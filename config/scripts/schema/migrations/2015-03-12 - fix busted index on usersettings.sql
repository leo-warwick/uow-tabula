DROP INDEX IDX_USERSETTINGS_USERID;
CREATE UNIQUE INDEX IDX_USERSETTINGS_USERID ON USERSETTINGS (USERID);