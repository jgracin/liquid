(ns dk.salza.liq.adapters.ttyadapter
  (:require [dk.salza.liq.apis :refer :all]
            [dk.salza.liq.util :as util]
            [clojure.string :as str]))

(def esc (str "\033" "["))

(defn raw2keyword
  [raw]
  (let [k (str (char raw))]
     (cond (re-matches #"[a-zA-Z0-9]" k) (keyword k)
           (= k "\t") :tab
           (= k " ") :space
           (= raw 13) :enter
           (= raw 33) :exclamation
           (= raw 34) :quote
           (= raw 35) :hash
           (= raw 36) :dollar
           (= raw 37) :percent
           (= raw 38) :ampersand
           (= raw 39) :singlequote
           (= raw 40) :parenstart
           (= raw 41) :parenend
           (= raw 42) :asterisk
           (= raw 43) :plus
           (= raw 44) :comma
           (= raw 45) :dash
           (= raw 46) :dot
           (= raw 47) :slash
           (= raw 58) :colon
           (= raw 59) :semicolon
           (= raw 60) :lt
           (= raw 61) :equal
           (= raw 62) :gt
           (= raw 63) :question
           (= raw 64) :at
           (= raw 91) :bracketstart
           (= raw 92) :backslash
           (= raw 93) :bracketend
           (= raw 94) :hat
           (= raw 95) :underscore
           ;(= raw 105) :up           
           ;(= raw 106) :left           
           ;(= raw 107) :down           
           ;(= raw 108) :right           
           (= raw 123) :bracesstart
           (= raw 124) :pipe
           (= raw 125) :bracesend
           (= raw 126) :tilde
           (= raw 164) :curren
           (= raw 197) :caa
           (= raw 198) :cae
           (= raw 216) :coe
           (= raw 229) :aa
           (= raw 230) :ae
           (= raw 248) :oe
           (= raw 5) :C-e
           (= raw 6) :C-f
           (= raw 7) :C-g
           (= raw 8) :C-h
           (= raw 10) :C-j
           (= raw 11) :C-k
           (= raw 12) :C-l
           (= raw 14) :C-n
           (= raw 15) :C-o
           (= raw 15) :C-p
           (= raw 17) :C-q
           (= raw 18) :C-r
           (= raw 19) :C-s
           (= raw 20) :C-t
           (= raw 23) :C-w
           (= raw 0) :C-space
           (= raw 127) :backspace
           true :unknown)))

(defn ttyrows
  []
  (let [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty"))]
    (Integer/parseInt (re-find #"^\d+" shellinfo)))) ; (re-find #"\d+$" "50 120")

(defn ttycolumns
  []
  (let [shellinfo (with-out-str (util/cmd "/bin/sh" "-c" "stty size </dev/tty"))]
    (Integer/parseInt (re-find #"\d+$" shellinfo)))) ; (re-find #"\d+$" "50 120")

(defn ttywait-for-input
  []
  ;(util/cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (let [input (.read (java.io.BufferedReader. *in*))]
    ;(spit "/tmp/keys.txt" (str input " - " "" (raw2keyword input) "\n") :append true)
    (raw2keyword input)))

(def old-lines (atom {}))
;Black       0;30     Dark Gray     1;30
;Blue        0;34     Light Blue    1;34
;Green       0;32     Light Green   1;32
;Cyan        0;36     Light Cyan    1;36
;Red         0;31     Light Red     1;31
;Purple      0;35     Light Purple  1;35
;Brown       0;33     Yellow        1;33
;Light Gray  0;37     White         1;37
;; http://misc.flogisoft.com/bash/tip_colors_and_formatting

(defn ttyprint-lines
  [lines]
  ;; Redraw whole screen once in a while
  ;; (when (= (rand-int 100) 0)
  ;;  (reset! old-lines {})
  ;;  (print "\033[0;37m\033[2J"))
  (doseq [line lines]
    (let [row (line :row)
          column (line :column)
          content (line :line)
          key (str "k" row "-" column)
          oldcontent (@old-lines key)] 
    (when (not= oldcontent content)
      (let [diff (max 1 (- (count (filter #(and (string? %) (not= % "")) oldcontent))
                           (count (filter #(and (string? %) (not= % "")) content))))
            padding (format (str "%" diff "s") " ")]
        (print (str "\033[" row ";" column "H\033[s" "\033[48;5;235m \033[0;37m\033[49m"))

        (doseq [ch (line :line)]
          (if (string? ch)
            (if (= ch "\t") (print (char 172)) (print ch)) 
            (do
              (cond (= (ch :face) :string) (print "\033[38;5;131m")
                    (= (ch :face) :comment) (print "\033[38;5;105m")
                    (= (ch :face) :type1) (print "\033[38;5;11m") ; defn
                    (= (ch :face) :type2) (print "\033[38;5;40m") ; function
                    (= (ch :face) :type3) (print "\033[38;5;117m") ; keyword
                    :else (print "\033[0;37m"))
              (cond (= (ch :bgface) :cursor1) (print "\033[42m")
                    (= (ch :bgface) :cursor2) (print "\033[44m")
                    (= (ch :bgface) :selection) (print "\033[48;5;52m")
                    (= (ch :bgface) :statusline) (print "\033[48;5;235m")
                    :else (print "\033[49m"))
            )))
        (if (= row (count lines))
          (print (str "  " padding))
          (print (str "\033[0;37m\033[49m" padding))))
      (swap! old-lines assoc key content))
    ))
  (flush))

(defn ttyreset
  []
  (reset! old-lines {})
  (print "\033[0;37m\033[2J"))
  

(defn ttyinit
  []
  (util/cmd "/bin/sh" "-c" "stty -echo raw </dev/tty")
  (print "\033[0;37m\033[2J")
  (print "\033[?25l") ; Hide cursor
  (print "\033[?7l") ; disable line wrap
  )

(defn ttyquit
  []
  ;(print "\033[0;37m\033[2J")
  ;(print "\033[c")
  ;(print "\033[?25h")
  ;(flush)
  (util/cmd "/bin/sh" "-c" "stty -echo cooked </dev/tty")
  (util/cmd "/bin/sh" "-c" "stty -echo reset </dev/tty")
  ;(util/cmd "reset")
  (System/exit 0))

(defrecord TtyAdapter []
  Adapter
  (init [this] (ttyinit))
  (rows [this] (ttyrows))
  (columns [this] (ttycolumns))
  (wait-for-input [this] (ttywait-for-input))
  (print-lines [this lines] (ttyprint-lines lines))
  (reset [this] (ttyreset))
  (quit [this] (ttyquit)))