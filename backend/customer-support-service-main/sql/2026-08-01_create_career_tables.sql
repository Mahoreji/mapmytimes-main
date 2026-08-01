-- Career module tables for customer-support-service.
-- Run on the customer-support-service database BEFORE starting the service.
-- Customer-support-service uses spring.jpa.hibernate.ddl-auto=validate
-- (i.e. Hibernate only checks that entity columns match DB — never auto-creates tables).

CREATE TABLE IF NOT EXISTS job_postings (
    id                      UUID PRIMARY KEY,
    title                   TEXT NOT NULL,
    department              TEXT NOT NULL,
    location                TEXT NOT NULL,
    job_type                VARCHAR(20) NOT NULL CHECK (job_type IN ('FULL_TIME','PART_TIME','INTERNSHIP','CONTRACT','FREELANCE')),
    experience_level        VARCHAR(20) NOT NULL CHECK (experience_level IN ('FRESHER','JUNIOR','MID','SENIOR','LEAD')),
    description             TEXT NOT NULL,
    requirements            TEXT,
    responsibilities        TEXT,
    salary_min              BIGINT,
    salary_max              BIGINT,
    salary_currency         VARCHAR(10) DEFAULT 'INR',
    is_active               BOOLEAN NOT NULL DEFAULT TRUE,
    application_deadline    DATE,
    posted_by               TEXT NOT NULL,
    created_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_postings_active      ON job_postings (is_active);
CREATE INDEX IF NOT EXISTS idx_job_postings_dept        ON job_postings (department);
CREATE INDEX IF NOT EXISTS idx_job_postings_job_type    ON job_postings (job_type);
CREATE INDEX IF NOT EXISTS idx_job_postings_exp_level   ON job_postings (experience_level);
CREATE INDEX IF NOT EXISTS idx_job_postings_created     ON job_postings (created_at DESC);

CREATE TABLE IF NOT EXISTS job_applications (
    id                       UUID PRIMARY KEY,
    job_id                   TEXT NOT NULL,
    applicant_id             TEXT NOT NULL,
    applicant_name           TEXT NOT NULL,
    applicant_email          TEXT NOT NULL,
    applicant_phone          TEXT NOT NULL,
    resume_url               TEXT,
    resume_s3_key            TEXT,
    resume_original_file_name TEXT,
    cover_letter             TEXT,
    status                   VARCHAR(25) NOT NULL DEFAULT 'APPLIED' CHECK (status IN ('APPLIED','UNDER_REVIEW','SHORTLISTED','INTERVIEW','INTERVIEW_SCHEDULED','REJECTED','SELECTED','WITHDRAWN')),
    current_ctc              TEXT,
    expected_ctc             TEXT,
    notice_period            TEXT,
    years_of_experience      INTEGER,
    admin_notes              TEXT,
    rejection_reason         TEXT,
    interview_scheduled_at   TIMESTAMP,
    created_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at               TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_job_applications_job_id     ON job_applications (job_id);
CREATE INDEX IF NOT EXISTS idx_job_applications_applicant  ON job_applications (applicant_id);
CREATE INDEX IF NOT EXISTS idx_job_applications_status     ON job_applications (status);
CREATE INDEX IF NOT EXISTS idx_job_applications_created    ON job_applications (created_at DESC);
