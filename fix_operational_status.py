import psycopg2
import os
from dotenv import load_dotenv

# Load environment variables
load_dotenv()

# Database connection parameters
conn_params = {
    'host': os.getenv('DB_HOST'),
    'port': os.getenv('DB_PORT'),
    'database': os.getenv('DB_NAME'),
    'user': os.getenv('DB_USER'),
    'password': os.getenv('DB_PASSWORD'),
    'sslmode': os.getenv('DB_SSLMODE')
}

print("Connecting to database...")
conn = psycopg2.connect(**conn_params)
cursor = conn.cursor()

print("\nStep 1: Check if column exists...")
cursor.execute("""
    SELECT column_name, data_type, is_nullable, column_default 
    FROM information_schema.columns 
    WHERE table_name = 'buses' AND column_name = 'operational_status';
""")
result = cursor.fetchone()
if result:
    print(f"  Column exists: {result}")
else:
    print("  Column does NOT exist")

print("\nStep 2: Adding column with default value...")
try:
    cursor.execute("""
        ALTER TABLE buses 
        ADD COLUMN IF NOT EXISTS operational_status VARCHAR(20) DEFAULT 'active';
    """)
    conn.commit()
    print("  ✓ Column added")
except Exception as e:
    print(f"  Note: {e}")
    conn.rollback()

print("\nStep 3: Update NULL values to 'active'...")
try:
    cursor.execute("""
        UPDATE buses 
        SET operational_status = 'active' 
        WHERE operational_status IS NULL;
    """)
    rows_updated = cursor.rowcount
    conn.commit()
    print(f"  ✓ Updated {rows_updated} rows")
except Exception as e:
    print(f"  Error: {e}")
    conn.rollback()

print("\nStep 4: Make column NOT NULL...")
try:
    cursor.execute("""
        ALTER TABLE buses 
        ALTER COLUMN operational_status SET NOT NULL;
    """)
    conn.commit()
    print("  ✓ Column set to NOT NULL")
except Exception as e:
    print(f"  Note: {e}")
    conn.rollback()

print("\nStep 5: Add check constraint...")
try:
    cursor.execute("""
        ALTER TABLE buses 
        DROP CONSTRAINT IF EXISTS check_operational_status;
    """)
    cursor.execute("""
        ALTER TABLE buses 
        ADD CONSTRAINT check_operational_status 
        CHECK (operational_status IN ('active', 'inactive', 'maintenance', 'retired'));
    """)
    conn.commit()
    print("  ✓ Check constraint added")
except Exception as e:
    print(f"  Error: {e}")
    conn.rollback()

print("\nStep 6: Verify final state...")
cursor.execute("""
    SELECT column_name, data_type, is_nullable, column_default 
    FROM information_schema.columns 
    WHERE table_name = 'buses' AND column_name = 'operational_status';
""")
result = cursor.fetchone()
print(f"  Final state: {result}")

cursor.execute("""
    SELECT operational_status, COUNT(*) as count 
    FROM buses 
    GROUP BY operational_status;
""")
results = cursor.fetchall()
print(f"\n  Status distribution:")
for row in results:
    print(f"    {row[0]}: {row[1]} buses")

cursor.close()
conn.close()
print("\n✓ Migration completed successfully!")
