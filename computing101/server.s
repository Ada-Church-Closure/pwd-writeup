.intel_syntax noprefix
.global _start

.section .data # 提前准备write的参数
response: .ascii "HTTP/1.0 200 OK\r\n\r\n"
resp_len = $ - response
request_file_len: .long 0

.section .text
.lcomm request, 1024
.lcomm file_path 1024
.lcomm file_buffer 1024

_start:
mov rdi, 2  # 创建一个server的socket
mov rsi, 1
mov rdx, 0
mov rax, 0x29
syscall

mov rbx, rax # socket的文件描述符号先放到rbx中
sub rsp, 16  # 创建struct sockaddr_in结构体的内容
mov word ptr [rsp], 0x0002
mov word ptr [rsp + 2], 0x5000
mov dword ptr [rsp + 4], 0x00000000
mov qword ptr [rsp + 8], 0

mov rdi, rbx    # socket的文件描述符号
lea rsi, [rsp]  # 结构体的地址
mov rdx, 16     # 结构体的大小
mov rax, 49     # 调用bind()
syscall

mov rdi, rbx    # listen，监听客户端
mov rsi, 0
mov rax, 50
syscall



mov rdi, rbx    # accpet，和发起请求的客户端连接,返回值是新建立的socket的FD
xor rsi, rsi
xor rdx, rdx
mov rax, 43
syscall

mov rbx, rax    # read调用，先读取client的请求,给我们一个文件的路径
mov rdi, rbx
lea rsi, [request]
mov rdx, 1024
mov rax, 0
syscall

lea rsi, [request]       
lea rdi, [file_path]     

.skip_spaces:
    mov al, byte ptr [rsi]
    cmp al, ' '             
    je .found_path
    inc rsi
    jmp .skip_spaces

.found_path:
    inc rsi                  

.copy_loop:
    mov al, byte ptr [rsi]
    cmp al, ' '
    je .done_copy
    cmp al, 13              
    je .done_copy
    mov byte ptr [rdi], al
    inc dword ptr [request_file_len]
    inc rsi
    inc rdi
    jmp .copy_loop

.done_copy:
    mov byte ptr [rdi], 0  


lea rdi, [file_path] 
mov rsi, 0
mov rdx, 0
mov rax, 2
syscall
mov r12, rax

mov rdi, rax       # 读取请求的文件到某个缓冲区内部, read
lea rsi, [file_buffer]
mov rdx, request_file_len
mov rax, 0
syscall
mov r13, rax       # 这里要先保存read到的字节数，用来确定最后要写入的字节数大小

mov rdi, r12       # close，关闭关于这个文件的读取
mov rax, 3
syscall


mov rdi, rbx    # write，向客户端写入数据,OK回应
mov rsi, offset response
mov rdx, resp_len
mov rax, 1
syscall

mov rdi, rbx
lea rsi, [file_buffer] # write,把请求文件文件中的内容写给socket
mov rdx, r13
mov rax, 1
syscall

mov rdi, rbx # 关闭socket
mov rax, 3
syscall

mov rdi, 0  # 退出这个程序
mov rax, 60
syscall

