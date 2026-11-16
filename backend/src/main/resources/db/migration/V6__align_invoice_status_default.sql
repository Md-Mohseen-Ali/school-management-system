-- Keep newly created invoices in the same initial state as the domain model.
-- Existing records are left unchanged; this is safe for databases that already
-- applied V1–V5 on earlier deployments.
ALTER TABLE fee_invoices
    ALTER COLUMN status SET DEFAULT 'DRAFT';

COMMENT ON COLUMN fee_invoices.status IS
    'Invoice lifecycle: DRAFT → ISSUED or CANCELLED. New invoices default to DRAFT.';
