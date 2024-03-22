DROP PROCEDURE IF EXISTS GetClanLogsByClanUUID;
CREATE PROCEDURE GetClanLogsByClanUUID(ClanUuid varchar(36), amount int)
BEGIN
   SELECT DISTINCT Time, Message
   FROM logs
   WHERE Message LIKE CONCAT('%', ClanUuid, '%')
   ORDER BY Time DESC
   LIMIT amount;
END;





