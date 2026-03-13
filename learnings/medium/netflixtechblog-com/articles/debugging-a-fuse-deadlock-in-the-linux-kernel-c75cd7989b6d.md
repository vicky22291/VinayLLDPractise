---
title: "Debugging a FUSE deadlock in the Linux kernel"
author: "Netflix Technology Blog"
date: "May 19, 2023"
url: "https://netflixtechblog.com/debugging-a-fuse-deadlock-in-the-linux-kernel-c75cd7989b6d"
tags: ['Linux', 'Deadlock', 'Docker', 'Fuse']
---

# Debugging a FUSE deadlock in the Linux kernel

[Tycho Andersen](https://tycho.pizza/)

The Compute team at Netflix is charged with managing all AWS and containerized workloads at Netflix, including autoscaling, deployment of containers, issue remediation, etc. As part of this team, I work on fixing strange things that users report.

This particular issue involved a custom internal [FUSE filesystem](https://www.kernel.org/doc/html/latest/filesystems/fuse.html): [ndrive](./netflix-drive-a607538c3055.md). It had been festering for some time, but needed someone to sit down and look at it in anger. This blog post describes how I poked at `/proc`to get a sense of what was going on, before posting the issue to the kernel mailing list and getting schooled on how the kernel’s wait code actually works!

## Symptom: Stuck Docker Kill & A Zombie Process

We had a stuck docker API call:

```
goroutine 146 [select, 8817 minutes]:
net/http.(*persistConn).roundTrip(0xc000658fc0, 0xc0003fc080, 0x0, 0x0, 0x0)
        /usr/local/go/src/net/http/transport.go:2610 +0x765
net/http.(*Transport).roundTrip(0xc000420140, 0xc000966200, 0x30, 0x1366f20, 0x162)
        /usr/local/go/src/net/http/transport.go:592 +0xacb
net/http.(*Transport).RoundTrip(0xc000420140, 0xc000966200, 0xc000420140, 0x0, 0x0)
        /usr/local/go/src/net/http/roundtrip.go:17 +0x35
net/http.send(0xc000966200, 0x161eba0, 0xc000420140, 0x0, 0x0, 0x0, 0xc00000e050, 0x3, 0x1, 0x0)
        /usr/local/go/src/net/http/client.go:251 +0x454
net/http.(*Client).send(0xc000438480, 0xc000966200, 0x0, 0x0, 0x0, 0xc00000e050, 0x0, 0x1, 0x10000168e)
        /usr/local/go/src/net/http/client.go:175 +0xff
net/http.(*Client).do(0xc000438480, 0xc000966200, 0x0, 0x0, 0x0)
        /usr/local/go/src/net/http/client.go:717 +0x45f
net/http.(*Client).Do(...)
        /usr/local/go/src/net/http/client.go:585
golang.org/x/net/context/ctxhttp.Do(0x163bd48, 0xc000044090, 0xc000438480, 0xc000966100, 0x0, 0x0, 0x0)
        /go/pkg/mod/golang.org/x/net@v0.0.0-20211209124913-491a49abca63/context/ctxhttp/ctxhttp.go:27 +0x10f
github.com/docker/docker/client.(*Client).doRequest(0xc0001a8200, 0x163bd48, 0xc000044090, 0xc000966100, 0x0, 0x0, 0x0, 0x0, 0x0, 0x0, ...)
        /go/pkg/mod/github.com/moby/moby@v0.0.0-20190408150954-50ebe4562dfc/client/request.go:132 +0xbe
github.com/docker/docker/client.(*Client).sendRequest(0xc0001a8200, 0x163bd48, 0xc000044090, 0x13d8643, 0x3, 0xc00079a720, 0x51, 0x0, 0x0, 0x0, ...)
        /go/pkg/mod/github.com/moby/moby@v0.0.0-20190408150954-50ebe4562dfc/client/request.go:122 +0x156
github.com/docker/docker/client.(*Client).get(...)
        /go/pkg/mod/github.com/moby/moby@v0.0.0-20190408150954-50ebe4562dfc/client/request.go:37
github.com/docker/docker/client.(*Client).ContainerInspect(0xc0001a8200, 0x163bd48, 0xc000044090, 0xc0006a01c0, 0x40, 0x0, 0x0, 0x0, 0x0, 0x0, ...)
        /go/pkg/mod/github.com/moby/moby@v0.0.0-20190408150954-50ebe4562dfc/client/container_inspect.go:18 +0x128
github.com/Netflix/titus-executor/executor/runtime/docker.(*DockerRuntime).Kill(0xc000215180, 0x163bdb8, 0xc000938600, 0x1, 0x0, 0x0)
        /var/lib/buildkite-agent/builds/ip-192-168-1-90-1/netflix/titus-executor/executor/runtime/docker/docker.go:2835 +0x310
github.com/Netflix/titus-executor/executor/runner.(*Runner).doShutdown(0xc000432dc0, 0x163bd10, 0xc000938390, 0x1, 0xc000b821e0, 0x1d, 0xc0005e4710)
        /var/lib/buildkite-agent/builds/ip-192-168-1-90-1/netflix/titus-executor/executor/runner/runner.go:326 +0x4f4
github.com/Netflix/titus-executor/executor/runner.(*Runner).startRunner(0xc000432dc0, 0x163bdb8, 0xc00071e0c0, 0xc0a502e28c08b488, 0x24572b8, 0x1df5980)
        /var/lib/buildkite-agent/builds/ip-192-168-1-90-1/netflix/titus-executor/executor/runner/runner.go:122 +0x391
created by github.com/Netflix/titus-executor/executor/runner.StartTaskWithRuntime
        /var/lib/buildkite-agent/builds/ip-192-168-1-90-1/netflix/titus-executor/executor/runner/runner.go:81 +0x411
```

Here, our management engine has made an HTTP call to the Docker API’s unix socket asking it to kill a container. Our containers are configured to be killed via `SIGKILL`. But this is strange. `kill(SIGKILL)` should be relatively fatal, so what is the container doing?

```
$ docker exec -it 6643cd073492 bash
OCI runtime exec failed: exec failed: container_linux.go:380: starting container process caused: process_linux.go:130: executing setns process caused: exit status 1: unknown
```

Hmm. Seems like it’s alive, but `setns(2)` fails. Why would that be? If we look at the process tree via `ps awwfux`, we see:

```
\_ containerd-shim -namespace moby -workdir /var/lib/containerd/io.containerd.runtime.v1.linux/moby/6643cd073492ba9166100ed30dbe389ff1caef0dc3d35
|  \_ [docker-init]
|      \_ [ndrive] <defunct>
```

Ok, so the container’s init process is still alive, but it has one zombie child. What could the container’s init process possibly be doing?

```
# cat /proc/1528591/stack
[<0>] do_wait+0x156/0x2f0
[<0>] kernel_wait4+0x8d/0x140
[<0>] zap_pid_ns_processes+0x104/0x180
[<0>] do_exit+0xa41/0xb80
[<0>] do_group_exit+0x3a/0xa0
[<0>] __x64_sys_exit_group+0x14/0x20
[<0>] do_syscall_64+0x37/0xb0
[<0>] entry_SYSCALL_64_after_hwframe+0x44/0xae
```

It is in the process of exiting, but it seems stuck. The only child is the ndrive process in Z (i.e. “zombie”) state, though. Zombies are processes that have successfully exited, and are waiting to be reaped by a corresponding `wait()` syscall from their parents. So how could the kernel be stuck waiting on a zombie?

```
# ls /proc/1544450/task
1544450  1544574
```

Ah ha, there are two threads in the thread group. One of them is a zombie, maybe the other one isn’t:

```
# cat /proc/1544574/stack
[<0>] request_wait_answer+0x12f/0x210
[<0>] fuse_simple_request+0x109/0x2c0
[<0>] fuse_flush+0x16f/0x1b0
[<0>] filp_close+0x27/0x70
[<0>] put_files_struct+0x6b/0xc0
[<0>] do_exit+0x360/0xb80
[<0>] do_group_exit+0x3a/0xa0
[<0>] get_signal+0x140/0x870
[<0>] arch_do_signal_or_restart+0xae/0x7c0
[<0>] exit_to_user_mode_prepare+0x10f/0x1c0
[<0>] syscall_exit_to_user_mode+0x26/0x40
[<0>] do_syscall_64+0x46/0xb0
[<0>] entry_SYSCALL_64_after_hwframe+0x44/0xae
```

Indeed it is not a zombie. It is trying to become one as hard as it can, but it’s blocking inside FUSE for some reason. To find out why, let’s look at some kernel code. If we look at `[zap_pid_ns_processes()](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/kernel/pid_namespace.c?h=v5.19#n166)`, it does:

```
/*
 * Reap the EXIT_ZOMBIE children we had before we ignored SIGCHLD.
 * kernel_wait4() will also block until our children traced from the
 * parent namespace are detached and become EXIT_DEAD.
 */
do {
        clear_thread_flag(TIF_SIGPENDING);
        rc = kernel_wait4(-1, NULL, __WALL, NULL);
} while (rc != -ECHILD);
```

which is where we are stuck, but before that, it has done:

```
/* Don't allow any more processes into the pid namespace */
disable_pid_allocation(pid_ns);
```

which is why docker can’t `setns()` — the _namespace_ is a zombie. Ok, so we can’t `setns(2)`, but why are we stuck in `kernel_wait4()`? To understand why, let’s look at what the other thread was doing in FUSE’s `[request_wait_answer()](https://git.kernel.org/pub/scm/linux/kernel/git/torvalds/linux.git/tree/fs/fuse/dev.c?h=v5.19#n407)`:

```
/*
 * Either request is already in userspace, or it was forced.
 * Wait it out.
 */
wait_event(req->waitq, test_bit(FR_FINISHED, &req->flags));
```

Ok, so we’re waiting for an event (in this case, that userspace has replied to the FUSE flush request). But `zap_pid_ns_processes()`sent a `SIGKILL`! `SIGKILL` should be very fatal to a process. If we look at the process, we can indeed see that there’s a pending `SIGKILL`:

```
# grep Pnd /proc/1544574/status
SigPnd: 0000000000000000
ShdPnd: 0000000000000100
```

Viewing process status this way, you can see `0x100` (i.e. the 9th bit is set) under `ShdPnd`, which is the signal number corresponding to `SIGKILL`. Pending signals are signals that have been generated by the kernel, but have not yet been delivered to userspace. Signals are only delivered at certain times, for example when entering or leaving a syscall, or when waiting on events. If the kernel is currently doing something on behalf of the task, the signal may be pending. Signals can also be blocked by a task, so that they are never delivered. Blocked signals will show up in their respective pending sets as well. However, `man 7 signal` says: “The signals `SIGKILL` and `SIGSTOP` cannot be caught, blocked, or ignored.” But here the kernel is telling us that we have a pending `SIGKILL`, aka that it is being ignored even while the task is waiting!

## Red Herring: How do Signals Work?

Well that is weird. The wait code (i.e. `include/linux/wait.h`) is used everywhere in the kernel: semaphores, wait queues, completions, etc. Surely it knows to look for `SIGKILL`s. So what does `wait_event()` actually do? Digging through the macro expansions and wrappers, the meat of it is:

```
#define ___wait_event(wq_head, condition, state, exclusive, ret, cmd)           \
({                                                                              \
        __label__ __out;                                                        \
        struct wait_queue_entry __wq_entry;                                     \
        long __ret = ret;       /* explicit shadow */                           \
                                                                                \
        init_wait_entry(&__wq_entry, exclusive ? WQ_FLAG_EXCLUSIVE : 0);        \
        for (;;) {                                                              \
                long __int = prepare_to_wait_event(&wq_head, &__wq_entry, state);\
                                                                                \
                if (condition)                                                  \
                        break;                                                  \
                                                                                \
                if (___wait_is_interruptible(state) && __int) {                 \
                        __ret = __int;                                          \
                        goto __out;                                             \
                }                                                               \
                                                                                \
                cmd;                                                            \
        }                                                                       \
        finish_wait(&wq_head, &__wq_entry);                                     \
__out:  __ret;                                                                  \
})
```

So it loops forever, doing `prepare_to_wait_event()`, checking the condition, then checking to see if we need to interrupt. Then it does `cmd`, which in this case is `schedule()`, i.e. “do something else for a while”. `prepare_to_wait_event()` looks like:

```
long prepare_to_wait_event(struct wait_queue_head *wq_head, struct wait_queue_entry *wq_entry, int state)
{
        unsigned long flags;
        long ret = 0;

        spin_lock_irqsave(&wq_head->lock, flags);
        if (signal_pending_state(state, current)) {
                /*
                 * Exclusive waiter must not fail if it was selected by wakeup,
                 * it should "consume" the condition we were waiting for.
                 *
                 * The caller will recheck the condition and return success if
                 * we were already woken up, we can not miss the event because
                 * wakeup locks/unlocks the same wq_head->lock.
                 *
                 * But we need to ensure that set-condition + wakeup after that
                 * can't see us, it should wake up another exclusive waiter if
                 * we fail.
                 */
                list_del_init(&wq_entry->entry);
                ret = -ERESTARTSYS;
        } else {
                if (list_empty(&wq_entry->entry)) {
                        if (wq_entry->flags & WQ_FLAG_EXCLUSIVE)
                                __add_wait_queue_entry_tail(wq_head, wq_entry);
                        else
                                __add_wait_queue(wq_head, wq_entry);
                }
                set_current_state(state);
        }
        spin_unlock_irqrestore(&wq_head->lock, flags);

        return ret;
}
EXPORT_SYMBOL(prepare_to_wait_event);
```

It looks like the only way we can break out of this with a non-zero exit code is if `signal_pending_state()` is true. Since our call site was just `wait_event()`, we know that state here is `TASK_UNINTERRUPTIBLE`; the definition of `signal_pending_state()` looks like:

```
static inline int signal_pending_state(unsigned int state, struct task_struct *p)
{
        if (!(state & (TASK_INTERRUPTIBLE | TASK_WAKEKILL)))
                return 0;
        if (!signal_pending(p))
                return 0;

        return (state & TASK_INTERRUPTIBLE) || __fatal_signal_pending(p);
}
```

Our task is not interruptible, so the first if fails. Our task should have a signal pending, though, right?

```
static inline int signal_pending(struct task_struct *p)
{
        /*
         * TIF_NOTIFY_SIGNAL isn't really a signal, but it requires the same
         * behavior in terms of ensuring that we break out of wait loops
         * so that notify signal callbacks can be processed.
         */
        if (unlikely(test_tsk_thread_flag(p, TIF_NOTIFY_SIGNAL)))
                return 1;
        return task_sigpending(p);
}
```

As the comment notes, `TIF_NOTIFY_SIGNAL` isn’t relevant here, in spite of its name, but let’s look at `task_sigpending()`:

```
static inline int task_sigpending(struct task_struct *p)
{
        return unlikely(test_tsk_thread_flag(p,TIF_SIGPENDING));
}
```

Hmm. Seems like we should have that flag set, right? To figure that out, let’s look at how signal delivery works. When we’re shutting down the pid namespace in `zap_pid_ns_processes()`, it does:

```
group_send_sig_info(SIGKILL, SEND_SIG_PRIV, task, PIDTYPE_MAX);
```

which eventually gets to `__send_signal_locked()`, which has:

```
pending = (type != PIDTYPE_PID) ? &t->signal->shared_pending : &t->pending;
...
sigaddset(&pending->signal, sig);
...
complete_signal(sig, t, type);
```

Using `PIDTYPE_MAX` here as the type is a little weird, but it roughly indicates “this is very privileged kernel stuff sending this signal, you should definitely deliver it”. There is a bit of unintended consequence here, though, in that `__send_signal_locked()` ends up sending the `SIGKILL` to the shared set, instead of the individual task’s set. If we look at the `__fatal_signal_pending()` code, we see:

```
static inline int __fatal_signal_pending(struct task_struct *p)
{
        return unlikely(sigismember(&p->pending.signal, SIGKILL));
}
```

But it turns out this is a bit of a red herring ([although](https://lore.kernel.org/all/YuGUyayVWDB7R89i@tycho.pizza/) [it](https://lore.kernel.org/all/20220728091220.GA11207@redhat.com/) [took](https://lore.kernel.org/all/871qu6bjp3.fsf@email.froward.int.ebiederm.org/) [a](https://lore.kernel.org/all/8735elhy4u.fsf@email.froward.int.ebiederm.org/) [while](https://lore.kernel.org/all/87pmhofr1q.fsf@email.froward.int.ebiederm.org/) for me to understand that).

## How Signals Actually Get Delivered To a Process

To understand what’s really going on here, we need to look at `complete_signal()`, since it unconditionally adds a `SIGKILL` to the task’s pending set:

```
sigaddset(&t->pending.signal, SIGKILL);
```

but why doesn’t it work? At the top of the function we have:

```
/*
 * Now find a thread we can wake up to take the signal off the queue.
 *
 * If the main thread wants the signal, it gets first crack.
 * Probably the least surprising to the average bear.
 */
if (wants_signal(sig, p))
        t = p;
else if ((type == PIDTYPE_PID) || thread_group_empty(p))
        /*
         * There is just one thread and it does not need to be woken.
         * It will dequeue unblocked signals before it runs again.
         */
        return;
```

but as [Eric Biederman described](https://lore.kernel.org/all/877d4jbabb.fsf@email.froward.int.ebiederm.org/), basically every thread can handle a `SIGKILL` at any time. Here’s `wants_signal()`:

```
static inline bool wants_signal(int sig, struct task_struct *p)
{
        if (sigismember(&p->blocked, sig))
                return false;

        if (p->flags & PF_EXITING)
                return false;

        if (sig == SIGKILL)
                return true;

        if (task_is_stopped_or_traced(p))
                return false;

        return task_curr(p) || !task_sigpending(p);
}
```

So… if a thread is already exiting (i.e. it has `PF_EXITING`), it doesn’t want a signal. Consider the following sequence of events:

1. a task opens a FUSE file, and doesn’t close it, then exits. During that exit, the kernel dutifully calls `do_exit()`, which does the following:

```
exit_signals(tsk); /* sets PF_EXITING */
```

2. `do_exit()` continues on to `exit_files(tsk);`, which flushes all files that are still open, resulting in the stack trace above.

3. the pid namespace exits, and enters `zap_pid_ns_processes()`, sends a `SIGKILL` to everyone (that it expects to be fatal), and then waits for everyone to exit.

4. this kills the FUSE daemon in the pid ns so it can never respond.

5. `complete_signal()` for the FUSE task that was already exiting ignores the signal, since it has `PF_EXITING`.

6. Deadlock. Without manually aborting the FUSE connection, things will hang forever.

## Solution: don’t wait!

It doesn’t really make sense to wait for flushes in this case: the task is dying, so there’s nobody to tell the return code of `flush()` to. It also turns out that this bug can happen with several filesystems (anything that calls the kernel’s wait code in `flush()`, i.e. basically anything that talks to something outside the local kernel).

Individual filesystems will need to be patched in the meantime, for example the fix for FUSE is [here](https://github.com/torvalds/linux/commit/14feceeeb012faf9def7d313d37f5d4f85e6572b), which was released on April 23 in Linux 6.3.

While this blog post addresses FUSE deadlocks, there are definitely issues in the nfs code and elsewhere, which we have not hit in production yet, but almost certainly will. You can also see it as a [symptom of other filesystem bugs](https://lore.kernel.org/all/20230512225414.GE3223426@dread.disaster.area/). Something to look out for if you have a pid namespace that won’t exit.

This is just a small taste of the variety of strange issues we encounter running containers at scale at Netflix. Our team is hiring, so please reach out if you also love red herrings and kernel deadlocks!

---
**Tags:** Linux · Deadlock · Docker · Fuse
