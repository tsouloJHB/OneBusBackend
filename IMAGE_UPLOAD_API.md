# Bus Company Image Upload API

This document describes the new image upload functionality for bus companies.

## New Endpoints

### 1. Create Bus Company with Image
**POST** `/api/bus-companies` (multipart/form-data)

**Parameters:**
- `name` (required): Company name
- `registrationNumber` (required): Registration number
- `companyCode` (required): Company code
- `email` (optional): Email address
- `phone` (optional): Phone number
- `address` (optional): Address
- `city` (optional): City
- `postalCode` (optional): Postal code
- `country` (optional): Country
- `isActive` (optional): Active status (default: true)
- `image` (optional): Company logo image file

**Example using curl:**
```bash
curl -X POST http://localhost:8080/api/bus-companies \
  -F "name=Example Bus Company" \
  -F "registrationNumber=REG123456" \
  -F "companyCode=EBC" \
  -F "email=contact@example.com" \
  -F "phone=+1234567890" \
  -F "city=Example City" \
  -F "image=@/path/to/logo.png"
```

### 2. Update Bus Company with Image
**PUT** `/api/bus-companies/{id}` (multipart/form-data)

**Parameters:**
- All parameters are optional (only provide fields to update)
- `image` (optional): New company logo image file

**Example using curl:**
```bash
curl -X PUT http://localhost:8080/api/bus-companies/1 \
  -F "name=Updated Company Name" \
  -F "image=@/path/to/new-logo.png"
```

## Response Format

Both endpoints return a BusCompanyResponseDTO with the following additional fields:
- `imagePath`: Relative path to the stored image file
- `imageUrl`: Full URL to access the image

**Example Response:**
```json
{
  "id": 1,
  "name": "Example Bus Company",
  "registrationNumber": "REG123456",
  "companyCode": "EBC",
  "email": "contact@example.com",
  "phone": "+1234567890",
  "city": "Example City",
  "imagePath": "media/550e8400-e29b-41d4-a716-446655440000.png",
  "imageUrl": "/media/550e8400-e29b-41d4-a716-446655440000.png",
  "isActive": true,
  "busCount": 0,
  "createdAt": "2024-01-15T10:30:00",
  "updatedAt": "2024-01-15T10:30:00"
}
```

## Image Requirements

- **Supported formats:** JPEG, PNG, GIF, WebP
- **Maximum file size:** 10MB
- **Storage location:** `media/` directory in the project root
- **Naming:** Images are automatically renamed with UUID to prevent conflicts

## Accessing Images

Images can be accessed directly via HTTP:
```
http://localhost:8080/media/{filename}
```

## Error Responses

**400 Bad Request:**
- Invalid image file type
- File size exceeds 10MB
- Validation errors (duplicate registration number, etc.)

**500 Internal Server Error:**
- Failed to store image file
- Database errors

## Configuration

The following properties can be configured in `application.properties`:

```properties
# File upload configuration
spring.servlet.multipart.max-file-size=10MB
spring.servlet.multipart.max-request-size=10MB

# Custom upload directory
app.upload.dir=media
```

## Database Changes

The `bus_companies` table now includes:
- `image_path` (VARCHAR(255)): Stores the relative path to the image file

The existing JSON-based create endpoint remains unchanged for backward compatibility.