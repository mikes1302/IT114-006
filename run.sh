if [ "$2" = "server" ]; then
    java $1.Server.Server
elif [ "$2" = "client" ]; then
    java $1.Client.Client
	    # In Milestone3 changes Client to ClientUI
elif [ "$2" = "ui" ]; then
    java $1.Client.ClientUI
	    # In Milestone3 changes Client to ClientUI
else
    echo "Must specify client, server, or ui"
fi
