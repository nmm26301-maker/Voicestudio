"""Run in the selected engine's isolated remote container, one process only."""
import os
from server.single_engine_app import create_app

app = create_app(os.environ["VOICE_DEPLOY_ENGINE"])
