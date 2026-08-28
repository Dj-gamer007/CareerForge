export type NotificationType =
  | 'APPLICATION_SUBMITTED'
  | 'APPLICATION_UPDATED'
  | 'APPLICATION_UPDATE'
  | 'APPLICATION_SHORTLISTED'
  | 'APPLICATION_REJECTED'
  | 'APPLICATION_ACCEPTED'
  | 'INTERVIEW_INVITE'
  | 'INTERVIEW_SCHEDULED'
  | 'INTERVIEW_RESCHEDULED'
  | 'COMPANY_VERIFICATION_PENDING'
  | 'COMPANY_VERIFICATION_REJECTED'
  | 'COMPANY_VERIFIED'
  | 'JOB_POSTING_CLOSED'
  | 'JOB_POSTING_DRAFTED'
  | 'JOB_POSTING_ARCHIVED'
  | 'JOB_POSTING_PUBLISHED'
  | 'ACCOUNT_DISABLED'
  | 'SYSTEM_ALERT'
  | 'JOB_RECOMMENDATION';

export interface NotificationResponse {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  isRead?: boolean;
  createdAt: string;
  actorName?: string | null;
  actorUserId?: number | null;
  relatedEntityType?: string | null;
  relatedEntityId?: number | null;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
