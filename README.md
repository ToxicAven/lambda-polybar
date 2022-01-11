# Lambda-Polybar

[Lambda Client](https://github.com/lambda-client/lambda) module for [Polybar](https://github.com/jaagr/polybar)


## Usage

Make sure `polybar-lambda.py` is executable

``` bash
chmod +x polybar-lambda.py
```

Use it in your polybar `config` as

``` ini
[module/speedtest]
type = custom/script
exec = "/path/to/polybar-lambda.py"
```
