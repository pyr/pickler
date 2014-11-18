(ns org.spootnik.pickler
  "Just enough pickle for graphite"
  (:import java.nio.ByteBuffer
           java.nio.ByteOrder
           java.nio.channels.FileChannel
           java.net.URI
           java.nio.file.OpenOption
           java.nio.file.StandardOpenOption
           java.nio.file.Files
           java.nio.file.Paths))

(defn path->byte-buffer
  "read a file in a byte-buffer"
  [fs-path]
  (let [uri  (Paths/get (URI. (str "file://" fs-path)))
        opts (into-array OpenOption [StandardOpenOption/READ])
        file (FileChannel/open uri opts)
        bb   (ByteBuffer/allocate (.size file))]
    (.read file bb 0)
    (.rewind bb)
    (.order bb ByteOrder/LITTLE_ENDIAN)
    bb))

(defmulti opcode (fn [b _] b))

(defmethod opcode 0x80
  [_ bb]
  {:type :protocol
   :version (-> bb .get .byteValue)})

(defmethod opcode 0x5d
  [_ bb]
  {:type :startlist})

(defmethod opcode 0x71
  [_ bb]
  {:type :binput
   :index (-> bb .get .byteValue)})

(defmethod opcode 0x28
  [_ bb]
  {:type :mark})

(defmethod opcode 0x58
  [_ bb]
  (let [size (.getInt bb)
        ba   (byte-array size)]
    (dotimes [i size]
      (aset-byte ba i (unchecked-byte (bit-and (.get bb) 0xff))))
    {:type :unicode
     :size size
     :val (String. ba "UTF-8")}))

(defmethod opcode 0x54
  [_ bb]
  (let [size (.getInt bb)
        ba   (byte-array size)]
    (dotimes [i size]
      (aset-byte ba i (unchecked-byte (bit-and (.get bb) 0xff))))
    {:type :unicode
     :size size
     :val (String. ba "UTF-8")}))

(defmethod opcode 0x55
  [_ bb]
  (let [size (int (bit-and (.get bb) 0xff))
        ba   (byte-array size)]
    (dotimes [i size]
      (aset-byte ba i (unchecked-byte (bit-and (.get bb) 0xff))))
    {:type :unicode
     :size size
     :val (String. ba "UTF-8")}))

(defmethod opcode 0x4a
  [_ bb]
  (.order bb ByteOrder/LITTLE_ENDIAN)
  (let [val (bit-and (.getInt bb) 0xffffffff)]
    {:type :int :val val}))

(defmethod opcode 0x47
  [_ bb]
  (.order bb ByteOrder/BIG_ENDIAN)
  (let [val (.getDouble bb)]
    (.order bb ByteOrder/LITTLE_ENDIAN)
    {:type :double :val val}))

(defmethod opcode 0x74
  [_ bb]
  {:type :tuple})

(defmethod opcode 0x65
  [_ bb]
  {:type :append})

(defmethod opcode 0x2e
  [_ bb]
  {:type :stop})

(defmethod opcode :default
  [val bb]
  (throw (ex-info "invalid pickle data"
                  {:opcode val
                   :position (.position bb)
                   :remaining (.remaining bb)})))

(defn raw->ast
  "Convert binary data into a list of pickle opcodes and data"
  [bb]
  (lazy-seq
   (when (pos? (.remaining bb))
     (let [b    (bit-and 0xff (.get bb))
           elem (opcode b bb)]
       (cons elem (raw->ast bb))))))

(defn augment
  "Augment stack with opcode result"
  [stack {:keys [type val]}]
  (cond
    (#{:unicode :int :double} type) (conj stack val)
    (= :stop type)                  (partition 3 stack)
    :else                           stack))

(defn ast->metrics
  "Given a pickle AST as yielded by raw->ast, and assuming we're dealing
   with graphite pickled data, yield a list of metrics"
  [ast]
  (loop [stack          nil
         [opcode & ast] (rest ast)]
    (if opcode
      (recur (augment stack opcode) ast)
      stack)))
