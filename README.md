# visitera

Demo app created using Clojure, ClojureScript, Luminus web framework and Datomic.

## Prerequisites

You will need [Leiningen][1] 2.0 or above installed.

[1]: https://github.com/technomancy/leiningen

### Datomic Starter

Datomic 需要使用 Java 8 来启动，不然后面启动项目（`lein run`）时会报错退出。

#### 启动 Datomic Transactor

Datomic Starter是可以免费使用的，但是需要注册账号申请License，License是永久的，不过需要一年一次的维护和更新。

把 [Datomic Starter][2] 下载下来之后解压，创建配置文件配置好License之后就可以启动Datomic Transactor了。

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

[2]: https://www.datomic.com/get-datomic.html

## Running

To start a web server for the application, run:

    lein run 

这个时候访问 `http://localhost:3000/` 会看到需要编译 ClojureScript 的提示。

再开一个终端窗口编译 ClojureScript:

``` sh
shadow-cljs watch app
```

## REPL

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

浏览器页面会即时显示出`Hello world!`

不过 shadow-cljs 的这个 REPL 具体还能怎么用尚未可知TBD。

最后，编辑器也可以[集成][3]REPL来运行命令，可以不用在命令行执行。

[3]: https://github.com/aliaksandr-s/prototyping-with-clojure/blob/master/tutorial/chapter-03/03-Environment%20set%20up.md#integrating-repl-to-a-text-editor

## Data Modeling

> To conveniently experiment with schema modeling we need a function to reset a database and we want the ability to run it without restarting the whole application.

Another way of starting the application:

    lein repl
    (start)

然后为了能够方便试验，我们在 `/env/dev/clj/user.clj` 添加了一个重置数据库的方法 `(reset-db)`:

``` clojure
(defn reset-db
  "Delete database and restart application"
  []
  (delete-database)
  (restart))
```

这样，执行 `(reset-db)` 时，`resources/migrations/schema.edn` 通过 `install-schema` 会被重新应用一次。

### Schema and data

Schema:

`resources/migrations/schema.edn`

Transform [data][4]:

```
         http://cljson.com/                        transform_data.clj                               copy
all.json -----------------> resources/raw/data.edn -----------------> resources/raw/parsed-data.edn ---> resources/migrations/countries-data.edn
```

其中 `transform_data.clj` 里的内容可以复制到REPL中执行，或者以load-file的形式加载后执行，或者以脚本的方式执行：

    clj -M resources/raw/transform_data.clj

[4]: https://raw.githubusercontent.com/lukes/ISO-3166-Countries-with-Regional-Codes/master/all/all.json
[5]: http://cljson.com/

同样，`countries-data.edn` 也会在执行 `(reset-db)` 时应用。

## Registration and Authentication

## UI

## Going Live

