# websocket
simple websocket listener

Expects a config file located at `config/application.yml` with the following settings:

```yml
websocket:
  url: "wss://somehost/some/endpoint"
  asynctimeoutms: 1800000
  sessiontimeoutms: 1800000
  pingtimems: 60000
```
