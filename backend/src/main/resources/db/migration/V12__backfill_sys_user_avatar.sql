UPDATE sys_user u
SET avatar_attachment_id = b.avatar_attachment_id
FROM box_user b
WHERE b.user_id = u.id
  AND u.avatar_attachment_id IS NULL
  AND b.avatar_attachment_id IS NOT NULL;
