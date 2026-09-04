#include "demo_net_http_bind.h"
#include <network/netstack/net_http.h>

Http_Headers *Demo_Http_CreateHeaders(void) {
    return OH_Http_CreateHeaders();
}

void Demo_Http_DestroyHeadersPtr(void *headers) {
    Http_Headers *h = headers;
    OH_Http_DestroyHeaders(&h);
}

uint32_t Demo_Http_SetHeaderValue(Http_Headers *headers, const char *name, const char *value) {
    return OH_Http_SetHeaderValue(headers, name, value);
}

Http_Request *Demo_Http_CreateRequest(const char *url) {
    return OH_Http_CreateRequest(url);
}

int Demo_Http_Request(Http_Request *request, Http_ResponseCallback callback, Http_EventsHandler handler) {
    return OH_Http_Request(request, callback, handler);
}

void Demo_Http_DestroyPtr(Http_Request *request) {
    OH_Http_Destroy(&request);
}
