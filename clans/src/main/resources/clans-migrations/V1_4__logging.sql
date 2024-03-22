DROP PROCEDURE IF EXISTS GetClanLogsByClanUUID;
CREATE PROCEDURE GetClanLogsByClanUUID(ClanUuid varchar(36), amount int)
BEGIN
   SELECT DISTINCT Time, Message
   FROM logs
   WHERE Message LIKE CONCAT('%', ClanUuid, '%')
   ORDER BY Time DESC
   LIMIT amount;
END;

DROP PROCEDURE IF EXISTS GetClanJoinLeaveLogsByClanUUID;
CREATE PROCEDURE GetClanJoinLeaveLogsByClanUUID(ClanUuid varchar(36))
BEGIN
   SELECT DISTINCT Time, Message
   FROM logs
   WHERE Message LIKE CONCAT('% joined %(', ClanUUid, ')%')
   OR Message LIKE CONCAT('% left %(', ClanUUid, ')%')
   OR Message LIKE CONCAT('% kicked %(', ClanUUid, ')%')
   ORDER BY Time DESC;
END;
