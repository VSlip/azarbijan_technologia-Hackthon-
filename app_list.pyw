import PySimpleGUI as gui
import pygetwindow as win
import time
import requests
import json
import os
from dataclasses import dataclass

BASE_URL = "https://gjtcq-2401-4900-a833-4deb-59d6-7885-ebbe-2d8.a.free.pinggy.link"

USER_ID_FILE = "user_id.txt"

def register_device():
    """Register this desktop device and get a user_id from the server."""
    payload = {
        "device_type": "desktop",
        "name": "My Desktop App"
    }
    try:
        response = requests.post(f"{BASE_URL}/devices", json=payload)
        if response.status_code == 200:
            user_id = response.json().get("user_id")
            with open(USER_ID_FILE, "w") as f:
                f.write(user_id)
            print(f"✅ Registered device. user_id: {user_id}")
            return user_id
        else:
            print(f"❌ Failed to register device: {response.status_code}, {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"❌ Network error during registration: {e}")
    return None

def get_user_id():
    """Load user_id from file or register if not found."""
    if os.path.exists(USER_ID_FILE):
        with open(USER_ID_FILE, "r") as f:
            return f.read().strip()
    return register_device()

def send_log_to_api(log_entry, user_id):
    payload = {
        "user_id": user_id,
        "app_name": log_entry.title,
        "start_time": int(log_entry.start_time*1000),
        "end_time": int(log_entry.end_time*1000),
        "device_type": "desktop"
    }
    try:
        response = requests.post(f"{BASE_URL}/logs", json=payload)
        if response.status_code in (200, 201):
            print(f"📤 Log sent: {log_entry.title}")
        else:
            print(f"❌ Failed to send log: {response.status_code}, {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"❌ Network error during log send: {e}")

@dataclass
class AppLog:
    title: str
    start_time: float
    end_time: float = None
    duration: float = None

YOUR_USER_ID = get_user_id()
if not YOUR_USER_ID:
    print("❌ Could not obtain user_id. Exiting.")
    exit()

running_apps = {}
completed_log = []

headings = ['Application Title', 'Start Timestamp', 'End Timestamp', 'Duration (s)']
layout = [
    [gui.Text("Live Process Time Tracker")],
    [gui.Table(values=[], headings=headings, auto_size_columns=False,
               col_widths=[40, 15, 15, 10],
               justification='left', key='APPLIST', display_row_numbers=True)],
    [gui.Button("Exit")]
         ]
window = gui.Window("Time Tracker", layout, finalize=True)
window.minimize()

initial_titles = [w.title for w in win.getAllWindows() if w.title]
for title in initial_titles:
    running_apps[title] = time.time()

while True:
    event, values = window.read(timeout=10000)

    if event == gui.WIN_CLOSED or event == 'Exit':
        break

    current_titles = [w.title for w in win.getAllWindows() if w.title]
    current_time = time.time()


    for title in list(running_apps.keys()):
        if title not in current_titles:
            start_time = running_apps.pop(title)
            end_time = current_time
            duration = end_time - start_time
            log = AppLog(title, start_time, end_time, duration)
            completed_log.append(log)
            send_log_to_api(log, YOUR_USER_ID)


    for title in current_titles:
        if title and title not in running_apps:
            running_apps[title] = current_time


    table_data = [
        [log.title, int(log.start_time), int(log.end_time), int(log.duration)]
        for log in reversed(completed_log)
    ] + [
        [title, int(start_time), "Running...", int(current_time - start_time)]
        for title, start_time in running_apps.items()
    ]

    window['APPLIST'].update(values=table_data)

window.close()
