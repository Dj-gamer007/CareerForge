import { describe, it, expect } from 'vitest';
import { jobFormSchema } from '@/schemas/job.schema';

describe('Job Form Schema Validation', () => {
  it('validates a correct job creation payload', () => {
    const valid = jobFormSchema.safeParse({
      title: 'Senior Java Backend Engineer',
      description: 'We are seeking an experienced Java engineer for our core platform services.',
      location: 'Bangalore, India',
      workMode: 'HYBRID',
      jobType: 'FULL_TIME',
      experienceLevel: 'SENIOR_LEVEL',
      salaryMin: 1500000,
      salaryMax: 2500000,
      currency: 'INR',
      deadline: '2026-12-31',
      skills: [
        { skillId: 1, required: true, minimumProficiency: 'ADVANCED' },
        { skillId: 2, required: true, minimumProficiency: 'INTERMEDIATE' },
      ],
    });
    expect(valid.success).toBe(true);
  });

  it('rejects salaryMax less than salaryMin', () => {
    const invalid = jobFormSchema.safeParse({
      title: 'Senior Java Backend Engineer',
      description: 'We are seeking an experienced Java engineer for our core platform services.',
      workMode: 'REMOTE',
      jobType: 'FULL_TIME',
      experienceLevel: 'MID_LEVEL',
      salaryMin: 2000000,
      salaryMax: 1000000, // Invalid: max < min
      currency: 'INR',
      deadline: '2026-12-31',
      skills: [{ skillId: 1, required: true, minimumProficiency: 'INTERMEDIATE' }],
    });
    expect(invalid.success).toBe(false);
  });
});
