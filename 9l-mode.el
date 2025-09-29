;; 9l-mode.el - Mojor mode for editing 9l story files

;;; Code:

(defvar 9l-mode-hook nil)

(defvar 9l-mode-map
  (let ((9l-mode-map (make-sparse-keymap)))
    (define-key 9l-mode-map "\M-." '9l-follow-choice)
    ;; Page up and page down:
    (define-key 9l-mode-map (kbd "<next>") '9l-next-node)
    (define-key 9l-mode-map (kbd "<prior>") '9l-prev-node)
    9l-mode-map)
  "Keymap for `9l-mode'")

(defvar 9l-font-lock-keywords
  (list
   `(,(rx  line-start "=" (+ (or alnum "_" "-" ":")) "=") . font-lock-function-name-face)
   `(,(rx line-start ">" (+ (or alnum "_" "-" ":"))) . font-lock-variable-name-face)
   `(,(rx line-start "!" (+ (or alnum "_" "-"))) . font-lock-function-name-face)
   ;;`(,(rx line-start "|" (* not-newline)) . font-lock-comment-face)
   `(,(rx line-start "|" (* not-newline))
     (0 font-lock-comment-face t))
   )
  "Keyword highlighting specification for 9L mode.")

(defvar 9l-mode-syntax-table
  (let ((9l-mode-syntax-table (make-syntax-table)))
    9l-mode-syntax-table)
  "Syntax table for `9l-mode'")

;; See https://www.emacswiki.org/emacs/SampleMode
(define-derived-mode 9l-mode text-mode "9L"
  "Major mode for editing 9l story files."
  :syntax-table 9l-mode-syntax-table
  (setq-local comment-start "# ")
  (setq-local font-lock-defaults '(9l-font-lock-keywords))
  (setq-local fill-column 65)
  (visual-line-mode))

(provide '9l-mode)

(add-to-list 'auto-mode-alist '("\\.9l\\'" . 9l-mode))

;;; Helpers

(defun 9l-parse-choice ()
  "Return NIL if current line is not a choice. Otherwise return
two strings, the node id and the choice text."
  (let ((at-choice-line (equal ?> (char-after (line-beginning-position)))))
    (when (not at-choice-line)
      (return-from 9l-parse-choice (cl-values nil nil))))
  (let ((line (buffer-substring-no-properties (line-beginning-position)
                                              (line-end-position))))
    (string-match (rx ">"
                      (group (+ (or alnum "_" "-")))
                      (? space)
                      (group (* any)))
                  line)
    (cl-values (format "=%s=" (match-string 1 line)) (match-string 2 line))))

(defun 9l-at-node-p (node-id)
  (let* ((len (length node-id))
         (start (line-beginning-position))
         (end (min (line-end-position) (+ start len))))
    (and (char-after start)
         (equal node-id (buffer-substring-no-properties start end)))))

(defun 9l-at-node-line-p ()
  (equal "=" (string (char-after (line-beginning-position)))))

(defun 9l-find-next-node (n)
  "If N is positive, finds next node. If N is negative, finds previous node."
  (if (>= n 0)
      (setq n 1)
    (setq n -1))
  (save-mark-and-excursion
    (when (9l-at-node-line-p)
      (forward-line n))
    (when (= (point) (point-max))
      (return-from 9l-find-next-node nil))
    (cond ((9l-at-node-line-p) t)
          (t
           (while (and (not (9l-at-node-line-p))
                       (if (< n 0)
                           (not (= (line-beginning-position) (point-min)))
                         (not (= (line-end-position) (point-max)))))
             (forward-line n))))
    (when (9l-at-node-line-p)
      (line-beginning-position))))

(defun 9l-next-node ()
  (interactive)
  (let ((next-node-point (9l-find-next-node 1)))
    (when next-node-point
      (goto-char next-node-point)
      (recenter 0))))

(defun 9l-prev-node ()
  (interactive)
  (let ((prev-node-point (9l-find-next-node -1)))
    (when prev-node-point
      (goto-char prev-node-point)
      (recenter 0))))


(defun 9l-find-node (node-id)
  (save-mark-and-excursion
    (when (9l-at-node-p node-id)
      (return-from 9l-find-node t))
    (goto-char (point-min))
    (cond ((9l-at-node-p node-id) t)
          (t
           (while (and (not (9l-at-node-p node-id))
                       (not (= (line-end-position) (point-max))))
             (forward-line 1))))
    (when (9l-at-node-p node-id)
      (line-beginning-position))))

(defun 9l-goto-node (node-id)
  (let ((node-point (9l-find-node node-id)))
    (when node-point
      (goto-char node-point)
      (recenter 0)
      t)))

(defun 9l-follow-choice ()
  (interactive)
  (require 'xref)
  (cl-multiple-value-bind (node-id txt) (9l-parse-choice)
    (when (not node-id)
      (return-from 9l-parse-choice nil))
    (xref-push-marker-stack)
    (unless (9l-goto-node node-id)
      (end-of-buffer)
      (insert "\n\n")
      (insert node-id)
      (insert "\n\n")
      )))
