docker run --rm -e HTTP_PROXY -e HTTPS_PROXY -e http_proxy -e https_proxy -e NO_PROXY -e no_proxy -v "$PWD:/workspace" -w /workspace smsmail-builder
