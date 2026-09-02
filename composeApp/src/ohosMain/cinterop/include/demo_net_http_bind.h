#ifndef DEMO_NET_HTTP_BIND_H
#define DEMO_NET_HTTP_BIND_H

#include <stddef.h>
#include <stdint.h>
#include "network/netstack/net_http_type.h"

#ifdef __cplusplus
extern "C" {
#endif

Http_Headers *Demo_Http_CreateHeaders(void);
void Demo_Http_DestroyHeadersPtr(void *headers);
uint32_t Demo_Http_SetHeaderValue(Http_Headers *headers, const char *name, const char *value);
Http_Request *Demo_Http_CreateRequest(const char *url);
int Demo_Http_Request(Http_Request *request, Http_ResponseCallback callback, Http_EventsHandler handler);
void Demo_Http_DestroyPtr(Http_Request *request);

#ifdef __cplusplus
}
#endif

#endif
