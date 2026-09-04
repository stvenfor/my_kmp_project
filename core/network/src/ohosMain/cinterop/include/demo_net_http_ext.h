#ifndef DEMO_NET_HTTP_EXT_H
#define DEMO_NET_HTTP_EXT_H

#include <network/netstack/net_http.h>

/**
 * Some netstack builds read an upload body immediately after [Http_RequestOptions]
 * in memory (not yet in public headers for API 20). Keep [body] adjacent to [options].
 */
typedef struct Demo_HttpRequestOptions {
    Http_RequestOptions options;
    Http_Buffer body;
} Demo_HttpRequestOptions;

#endif
