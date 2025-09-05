import PySimpleGUI as gui
import pygetwindow as win
import time
import requests
from dataclasses import dataclass

def send_log_to_api(log_entry):
    api_url = "http://rrfku-2401-4900-1a57-314f-9d00-3d9c-50dc-3aa0.a.free.pinggy.link/api/logs"
    api_key = "YOUR_SECRET_API_KEY"

    payload = {
        "user_id": "your_username",
        "application_name": log_entry.title,
        "start_time_unix": int(log_entry.start_time),
        "end_time_unix": int(log_entry.end_time),
        "duration_seconds": int(log_entry.duration),
        "device_type": "desktop_windows"
    }
    
    headers = {
        "Content-Type": "application/json",
        "Authorization": f"Bearer {api_key}"
    }

    try:
        response = requests.post(api_url, json=payload, headers=headers)
        if response.status_code == 201:
            print(f"Successfully sent log for: {log_entry.title}")
        else:
            print(f"Failed to send log. Status: {response.status_code}, Response: {response.text}")
    except requests.exceptions.RequestException as e:
        print(f"A network error occurred: {e}")

@dataclass
class AppLog:
    title: str
    start_time: float
    end_time: float = None
    duration: float = None

running_apps = {}
completed_log = []

headings = ['Application Title', 'Start Timestamp', 'End Timestamp', 'Duration (s)']
layout = [
    [gui.Text("Live Process Time Tracker")],
    [gui.Table(values=[], headings=headings, auto_size_columns=False, 
               col_widths=[40, 15, 15, 10],
               justification='left', key='~APPLIST~', display_row_numbers=True)],
    [gui.Button("Exit")]
]

window = gui.Window("Time Tracker", layout, finalize=True)
window.minimize()

initial_titles = [w.title for w in win.getAllWindows() if w.title]
for title in initial_titles:
    if title:
        running_apps[title] = time.time()

while True:
    event, values = window.read(timeout=2000)

    if event == gui.WIN_CLOSED or event == 'Exit':
        break

    current_titles = [w.title for w in win.getAllWindows() if w.title]
    current_time = time.time()
    
    running_keys = list(running_apps.keys())
    for title in running_keys:
        if title not in current_titles:
            start_time = running_apps.pop(title)
            end_time = current_time
            duration = end_time - start_time
            log = AppLog(title, start_time, end_time, duration)
            completed_log.append(log)
            send_log_to_api(log)

    for title in current_titles:
        if title and title not in running_apps:
            running_apps[title] = current_time

    table_data = []
    
    for log in reversed(completed_log):
        table_data.append([
            log.title, 
            int(log.start_time), 
            int(log.end_time), 
            int(log.duration)
        ])
        
    for title, start_time in running_apps.items():
        table_data.append([
            title, 
            int(start_time), 
            "Running...", 
            int(current_time - start_time)
        ])

    window['~APPLIST~'].update(values=table_data)

window.close()