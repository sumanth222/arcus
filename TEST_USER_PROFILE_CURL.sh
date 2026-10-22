#!/bin/bash
# CURL Commands to Test User Profile API Endpoints
# Run these commands in your terminal to test the user profile system

echo "=========================================="
echo "USER PROFILE API - CURL TEST COMMANDS"
echo "=========================================="

# Test 1: Create User Profile
echo ""
echo "1. CREATE USER PROFILE"
echo "====================="
curl -X POST "http://localhost:8080/user/profile/create?userId=1&name=John%20Doe&email=john@example.com&level=beginner&fitnessGoal=muscle_gain" \
  -H "Content-Type: application/json" \
  -v

# Test 2: Get User Profile
echo ""
echo ""
echo "2. GET USER PROFILE"
echo "==================="
curl -X GET "http://localhost:8080/user/profile/1" \
  -H "Content-Type: application/json" \
  -v

# Test 3: Update User Profile
echo ""
echo ""
echo "3. UPDATE USER PROFILE"
echo "======================"
curl -X PUT "http://localhost:8080/user/profile/1?name=John%20Smith&bio=Fitness%20Enthusiast%20and%20Coach" \
  -H "Content-Type: application/json" \
  -v

# Test 4: Refresh User Statistics
echo ""
echo ""
echo "4. REFRESH USER STATISTICS"
echo "=========================="
curl -X GET "http://localhost:8080/user/profile/1/refresh" \
  -H "Content-Type: application/json" \
  -v

# Test 5: Get Profile Completeness
echo ""
echo ""
echo "5. GET PROFILE COMPLETENESS"
echo "==========================="
curl -X GET "http://localhost:8080/user/profile/1/completeness" \
  -H "Content-Type: application/json" \
  -v

echo ""
echo "=========================================="
echo "TEST COMPLETE"
echo "=========================================="

