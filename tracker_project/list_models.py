import os
from dotenv import load_dotenv
from google import genai

# Use the API Key from your tracker.py
load_dotenv()
API_KEY = os.getenv("GEMINI_API_KEY")
client = genai.Client(api_key=API_KEY)

print("Listing available models...")
# The method to call is client.models.list()
for model in client.models.list():
    print(f"Model: {model.name}")