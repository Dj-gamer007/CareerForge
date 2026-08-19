import { describe, it, expect } from 'vitest';
import { loginSchema, registerSchema } from '@/schemas/auth.schema';

describe('Auth Validation Schemas', () => {
  it('validates login credentials correctly', () => {
    const valid = loginSchema.safeParse({
      email: 'student@careerforge.local',
      password: 'DevPass123!',
    });
    expect(valid.success).toBe(true);

    const invalid = loginSchema.safeParse({
      email: 'not-an-email',
      password: '',
    });
    expect(invalid.success).toBe(false);
  });

  it('enforces password complexity and match on registration', () => {
    const valid = registerSchema.safeParse({
      email: 'candidate@careerforge.local',
      password: 'StrongPass123!',
      confirmPassword: 'StrongPass123!',
      role: 'ROLE_STUDENT',
    });
    expect(valid.success).toBe(true);

    const mismatch = registerSchema.safeParse({
      email: 'candidate@careerforge.local',
      password: 'StrongPass123!',
      confirmPassword: 'DifferentPass123!',
      role: 'ROLE_STUDENT',
    });
    expect(mismatch.success).toBe(false);

    const weakPassword = registerSchema.safeParse({
      email: 'candidate@careerforge.local',
      password: 'weak',
      confirmPassword: 'weak',
      role: 'ROLE_STUDENT',
    });
    expect(weakPassword.success).toBe(false);
  });
});
