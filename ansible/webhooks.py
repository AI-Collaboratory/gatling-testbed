import web
import json

urls = ('/.*', 'webhooks')

app = web.application(urls, globals())


class hooks:
    def POST(self):
        data = web.data()
        event = json.loads(data)
        # if runtests tag found, then log commit to a file.
        # TODO security: check that commit string contains only alphanumerics
        # TODO clear the 'G' at queue
        # TODO schedule command at night with batch (wait for low CPU load)
        "at -q G drastictests.sh <commit>"
        with open('/opt/somefile.txt', 'a') as the_file:
            the_file.write('DATA RECEIVED:')
            the_file.write(json.dumps(event, indent=2))
            the_file.write('\n')
        return 'OK'


if __name__ == '__main__':
    app.run()
