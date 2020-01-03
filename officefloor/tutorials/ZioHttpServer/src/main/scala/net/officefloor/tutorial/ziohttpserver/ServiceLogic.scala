/*-
 * #%L
 * ZIO Tutorial
 * %%
 * Copyright (C) 2005 - 2020 Daniel Sagenschneider
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 * #L%
 */

package net.officefloor.tutorial.ziohttpserver

import net.officefloor.plugin.section.clazz.Parameter
import net.officefloor.web.ObjectResponse
import zio.ZIO

/**
 * Logic to service request.
 */
class ServiceLogic {

  // START SNIPPET: service
  def service(request: ZioRequest, repository: MessageRepository): ZIO[Any, Throwable, Message] = {

    // Service Logic
    val zio = for {
      m <- MessageService.getMessage(request.id)
      // possible further logic
    } yield m

    // Provide environment from dependency injection
    zio.provide(new InjectMessageRepository {
      override val messageRepository = repository
    })
  }
  // END SNIPPET: service

  // START SNIPPET: send
  def send(@Parameter message: Message, response: ObjectResponse[ZioResponse]): Unit =
    response.send(new ZioResponse(message.getContent))
  // END SNIPPET: send
}
