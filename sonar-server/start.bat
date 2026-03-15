@echo off
set PYTHONPATH=.
uvicorn app.main:app --reload
