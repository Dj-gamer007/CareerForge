export type NotificationType =
  | 'APPLICATION_SUBMITTED'
  | 'APPLICATION_STATUS_CHANGED'
  | 'INTERVIEW_SCHEDULED'
  | 'JOB_MATCH_ALERT'
  | 'COMPANY_VERIFIED'
  | 'COMPANY_REJECTED'
  | 'JOB_MODERATED'
  | 'SYSTEM_ALERT';

export interface NotificationResponse {
  id: number;
  title: string;
  message: string;
  type: NotificationType;
  read: boolean;
  createdAt: string;
}

export interface UnreadCountResponse {
  unreadCount: number;
}
