export type AuditEventType =
  | 'USER_STATUS_UPDATED'
  | 'USER_SELF_DISABLE_REJECTED'
  | 'COMPANY_VERIFICATION_UPDATED'
  | 'JOB_MODERATION_PERFORMED'
  | 'JOB_PUBLISH_GUARD_BLOCKED'
  | 'ADMIN_LOGIN_SUCCESS'
  | 'ADMIN_LOGIN_FAILURE';

export type AuditTargetType = 'USER' | 'COMPANY' | 'JOB' | 'APPLICATION' | 'AUTH';
export type AuditStatus = 'SUCCESS' | 'FAILURE';

export interface AuditLogSummaryResponse {
  id: number;
  actorUserId?: number;
  actorEmail: string;
  actorRole: string;
  eventType: AuditEventType;
  targetEntityType: AuditTargetType;
  targetEntityId?: number;
  targetIdentifier?: string;
  status: AuditStatus;
  reason?: string;
  ipAddress?: string;
  createdAt: string;
}

export interface AuditLogDetailResponse extends AuditLogSummaryResponse {
  details?: string;
  userAgent?: string;
}
