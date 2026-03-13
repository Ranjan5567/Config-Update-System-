import requests
import json

BASE_URL = "http://localhost:8081/api"

def test_orchestrated_update_and_audit():
    merchant_id = 1001
    attribute = "active"
    new_value =  "false"
    user = "Python_Orchestrator"

    print(f"--- 1. Fetching Current Value for {attribute} ---")
    current_val = "null"
    try:
        fetch_payload = {
            "merchantId": merchant_id,
            "attribute": attribute
        }
        res = requests.post(f"{BASE_URL}/config/attribute-value", json=fetch_payload)
        if res.status_code == 200:
            current_val = str(res.json().get('value', 'null'))
            print(f"Current Value found: {current_val}")
    except Exception as e:
        print(f"Fetch failed (non-critical): {e}")

    print(f"\n--- 2. Triggering Update for {attribute} ---")
    # This payload must match the Java Record: UpdateValueRequest
    update_payload = {
        "merchantId": merchant_id,
        "attribute": attribute,
        "value": new_value
    }
    
    try:
        update_res = requests.post(f"{BASE_URL}/config/update-value", json=update_payload)
        
        if update_res.status_code == 200:
            print(f"SUCCESS: {update_res.json().get('message')}")
            
            print("\n--- 3. Triggering Audit Log Storage (Follow-up) ---")
            audit_payload = {
                "createdBy": user,
                "merchantId": merchant_id,
                "attributeChanged": attribute,
                "valueFrom": current_val,
                "valueTo": str(new_value)
            }
            audit_res = requests.post(f"{BASE_URL}/audit/store", json=audit_payload)
            
            if audit_res.status_code == 200:
                print(f"SUCCESS: {audit_res.json().get('message')}")
            else:
                print(f"Audit Store Failed: {audit_res.text}")
        else:
            print(f"Update Failed: {update_res.status_code} - {update_res.text}")
            
    except requests.exceptions.RequestException as e:
        print(f"Request Error: {e}")

if __name__ == "__main__":
    test_orchestrated_update_and_audit()
