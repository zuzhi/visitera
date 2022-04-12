# visitera

generated using Luminus version "4.25"

FIXME

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

### Datomic Pro Starter

Datomic 需要使用 Java 8 来启动，不然后面启动项目（`lein run`）时会报错退出。

#### 启动 Datomic Transactor

```sh
# Create config file from sample, and update `license-key`
cp config/samples/dev-transactor-template.properties config/dev-transactor-template.properties

# Start transactor
bin/transactor -Ddatomic.printConnectionInfo=true config/dev-transactor-template.properties

# (Optional)Start REPL
bin/repl

# (Optional)Start console
bin/console -p 8080 dev datomic:dev://localhost:4334
```

## Running

To start a web server for the application, run:

    lein run 

这个时候访问 `http://localhost:3000/` 会看到编译 ClojureScript 的提示。

编译 ClojureScript:

``` sh
shadow-cljs watch app
```

## Check, and REPL

访问 `http://localhost:3000/db-test` 应该可以看到：

`China`

这是因为我们新增了一个 `/db-test` 的 route来测试数据库集成：

``` clojure
["/db-test" {:get (fn [_]
                    (let [db (d/db conn)
                          country (find-country-by-alpha-3 db "CHN")]
                      (-> (response/ok (:country/name country))
                          (response/header "Content-Type" "text/plain; charset=utf-8"))))}]
```

但是这样测试相当繁琐，我们可以使用REPL来简化测试：

    lein repl :connect localhost:7000

``` clojure
;; Change namespace
(in-ns 'visitera.db.core)

;; Run the command
(:country/name (let [db (d/db conn)]
   (find-country-by-alpha-3 db "CHN")))
```

此外，`shadow-cljs watch app` 运行时也会启动一个 REPL server，端口为7002,可以通过 `shadow-cljs node-repl` 或 `shadow-cljs browser-repl` 来访问并执行命令。

    shadow-cljs browser-repl

browser-repl 会打开 http://localhost:9630, 并且Code entered in a browser-repl prompt will be evaluated here.

通过在 browser-repl 中运行以下命令：

``` clojure
(set! (.-innerHTML (.getElementById js/document "app")) "Hello world!")
```

浏览器页面会即时显示出Hello world!

不过 shadow-cljs 的这个 REPL 具体还能怎么用尚未可知TBD。

最后，编辑器也可以集成REPL来运行命令，可以不用在命令行执行。

## Data Modeling

> To conveniently experiment with schema modeling we need a function to reset a database and we want the ability to run it without restarting the whole application.

Another way of starting the application:

    lein repl
    (start)

### Creating a schema

Schema:

`resources/migrations/schema.edn`

Transform:

                         transform-data.clj
                         ----------------->
`resources/raw/data.edn`                    `resources/raw/parsed-data.edn`

## License

Copyright © 2021 FIXME
