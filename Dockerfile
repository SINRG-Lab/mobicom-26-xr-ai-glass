# Environment for regenerating the paper's figures. Build, then run:
#   docker build -t xr-ai-glass .
#   docker run --rm -v "$PWD":/artifact xr-ai-glass ./run_all_notebooks.sh
# The repository is bind-mounted rather than copied, so the image stays small.

FROM python:3.12-slim

# Python 3.12 matches the notebooks; all pins ship manylinux wheels, so no
# compiler is needed.
ENV PYTHONUNBUFFERED=1 \
    PIP_NO_CACHE_DIR=1 \
    PIP_DISABLE_PIP_VERSION_CHECK=1

# libgomp1 is required by scikit-learn's OpenMP-backed estimators.
RUN apt-get update \
 && apt-get install -y --no-install-recommends libgomp1 \
 && rm -rf /var/lib/apt/lists/*

WORKDIR /artifact

# Requirements first, so the dependency layer caches independently.
COPY requirements.txt /tmp/requirements.txt
RUN pip install -r /tmp/requirements.txt

# Non-root, so figures written to the bind mount are not owned by root.
ARG UID=1000
ARG GID=1000
RUN groupadd -g "$GID" reviewer 2>/dev/null || true \
 && useradd -m -u "$UID" -g "$GID" reviewer 2>/dev/null || true \
 && chown -R "$UID:$GID" /artifact
USER $UID:$GID

EXPOSE 8888

# Token auth stays ON -- this server executes arbitrary code. Set JUPYTER_TOKEN
# for a predictable URL.
CMD ["jupyter", "lab", \
     "--ip=0.0.0.0", "--port=8888", "--no-browser", \
     "--ServerApp.root_dir=/artifact"]
